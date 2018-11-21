/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.dbs;

import gabien.ui.IConsumer;
import r48.AppMain;
import r48.RubyIO;
import r48.io.IObjectBackend;
import r48.schema.SchemaElement;
import r48.schema.util.SchemaPath;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.WeakHashMap;

/**
 * Not quite a database, but not quite not a database either.
 * Created on 12/29/16.
 */
public class ObjectDB {
    private final IObjectBackend backend;
    private final IConsumer<String> saveHook;
    public String binderPrefix;

    public ObjectDB(IObjectBackend b, IConsumer<String> sv) {
        backend = b;
        binderPrefix = b.userspaceBindersPrefix();
        saveHook = sv;
    }

    public HashMap<String, WeakReference<RubyIO>> objectMap = new HashMap<String, WeakReference<RubyIO>>();
    public WeakHashMap<RubyIO, String> reverseObjectMap = new WeakHashMap<RubyIO, String>();
    // The values don't actually matter -
    //  this locks the object into memory for as long as it's modified.
    public HashSet<RubyIO> modifiedObjects = new HashSet<RubyIO>();
    public HashSet<RubyIO> newlyCreatedObjects = new HashSet<RubyIO>();
    public WeakHashMap<RubyIO, LinkedList<WeakReference<IConsumer<SchemaPath>>>> objectListenersMap = new WeakHashMap<RubyIO, LinkedList<WeakReference<IConsumer<SchemaPath>>>>();
    public HashMap<String, LinkedList<WeakReference<IConsumer<SchemaPath>>>> objectRootListenersMap = new HashMap<String, LinkedList<WeakReference<IConsumer<SchemaPath>>>>();

    private boolean objectRootModifiedRecursion = false;

    public String getIdByObject(RubyIO obj) {
        return reverseObjectMap.get(obj);
    }

    // NOTE: Preferably call the one-parameter version,
    //  since that tries to create a sensible default.
    public RubyIO getObject(String id, String backupSchema) {
        if (objectMap.containsKey(id)) {
            RubyIO r = objectMap.get(id).get();
            if (r != null)
                return r;
        }
        RubyIO rio = backend.loadObjectFromFile(id);
        if (rio == null) {
            if (backupSchema != null) {
                if (!AppMain.schemas.hasSDBEntry(backupSchema)) {
                    System.err.println("Could not find backup schema for object " + id);
                    return null;
                }
                SchemaElement ise = AppMain.schemas.getSDBEntry(backupSchema);
                if (ise != null) {
                    rio = new RubyIO().setNull();
                    SchemaPath.setDefaultValue(rio, ise, null);
                    modifiedObjects.add(rio);
                    newlyCreatedObjects.add(rio);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        objectMap.put(id, new WeakReference<RubyIO>(rio));
        reverseObjectMap.put(rio, id);
        return rio;
    }

    public RubyIO getObject(String id) {
        return getObject(id, "File." + id);
    }

    public void ensureSaved(String id, RubyIO rio) {
        if (objectMap.containsKey(id)) {
            RubyIO rio2 = objectMap.get(id).get();
            if (rio2 != null) {
                if (rio2 != rio) {
                    // Overwriting - clean up.
                    System.out.println("WARNING: Overwriting shouldn't really ever happen.");
                    modifiedObjects.remove(rio2);
                    newlyCreatedObjects.remove(rio2);
                }
            }
        }
        try {
            backend.saveObjectToFile(id, rio);
            objectMap.put(id, new WeakReference<RubyIO>(rio));
            reverseObjectMap.put(rio, id);
            modifiedObjects.remove(rio);
            newlyCreatedObjects.remove(rio);
        } catch (Exception ioe) {
            // ERROR!
            AppMain.launchDialog(TXDB.get("Error saving object: ") + id + "\n" + ioe);
            ioe.printStackTrace();
            return;
        }
        saveHook.accept(id);
    }

    public boolean getObjectModified(String id) {
        WeakReference<RubyIO> riow = objectMap.get(id);
        if (riow == null)
            return false;
        RubyIO potentiallyModified = riow.get();
        if (potentiallyModified != null)
            return modifiedObjects.contains(potentiallyModified);
        return false;
    }

    public boolean getObjectNewlyCreated(String id) {
        WeakReference<RubyIO> riow = objectMap.get(id);
        if (riow == null)
            return false;
        RubyIO potentiallyModified = riow.get();
        if (potentiallyModified != null)
            return newlyCreatedObjects.contains(potentiallyModified);
        return false;
    }

    private LinkedList<WeakReference<IConsumer<SchemaPath>>> getOrCreateModificationHandlers(RubyIO p) {
        LinkedList<WeakReference<IConsumer<SchemaPath>>> notifyObjectModified = objectListenersMap.get(p);
        if (notifyObjectModified == null) {
            notifyObjectModified = new LinkedList<WeakReference<IConsumer<SchemaPath>>>();
            objectListenersMap.put(p, notifyObjectModified);
        }
        return notifyObjectModified;
    }

    private LinkedList<WeakReference<IConsumer<SchemaPath>>> getOrCreateRootModificationHandlers(String p) {
        LinkedList<WeakReference<IConsumer<SchemaPath>>> notifyObjectModified = objectRootListenersMap.get(p);
        if (notifyObjectModified == null) {
            notifyObjectModified = new LinkedList<WeakReference<IConsumer<SchemaPath>>>();
            objectRootListenersMap.put(p, notifyObjectModified);
        }
        return notifyObjectModified;
    }

    // Note that these are run at the end of frame,
    //  because there appears to be a performance issue with these being spammed over and over again. Oops.
    // Also note, these are all weakly referenced.

    public void registerModificationHandler(RubyIO root, IConsumer<SchemaPath> handler) {
        getOrCreateModificationHandlers(root).add(new WeakReference<IConsumer<SchemaPath>>(handler));
    }

    public void deregisterModificationHandler(RubyIO root, IConsumer<SchemaPath> handler) {
        removeFromGOCMH(getOrCreateModificationHandlers(root), handler);
    }

    public void registerModificationHandler(String root, IConsumer<SchemaPath> handler) {
        getOrCreateRootModificationHandlers(root).add(new WeakReference<IConsumer<SchemaPath>>(handler));
    }

    public void deregisterModificationHandler(String root, IConsumer<SchemaPath> handler) {
        removeFromGOCMH(getOrCreateRootModificationHandlers(root), handler);
    }

    public void objectRootModified(final RubyIO p, final SchemaPath path) {
        if (objectRootModifiedRecursion) {
            AppMain.pendingRunnables.add(new Runnable() {
                @Override
                public void run() {
                    // in case of mysterious error
                    objectRootModifiedRecursion = false;
                    objectRootModified(p, path);
                }
            });
            return;
        }
        objectRootModifiedRecursion = true;
        // Is this available in ObjectDB? If not, then it shouldn't be locked into permanent memory.
        // However, if there are modification listeners on this particular object, they get used
        if (reverseObjectMap.containsKey(p))
            modifiedObjects.add(p);
        LinkedList<WeakReference<IConsumer<SchemaPath>>> notifyObjectModified = objectListenersMap.get(p);
        handleNotificationList(notifyObjectModified, path);
        String root = getIdByObject(path.findRoot().targetElement);
        if (root != null) {
            notifyObjectModified = objectRootListenersMap.get(root);
            handleNotificationList(notifyObjectModified, path);
        }
        objectRootModifiedRecursion = false;
    }

    private void handleNotificationList(LinkedList<WeakReference<IConsumer<SchemaPath>>> notifyObjectModified, SchemaPath sp) {
        if (notifyObjectModified == null)
            return;
        for (WeakReference<IConsumer<SchemaPath>> spi : new LinkedList<WeakReference<IConsumer<SchemaPath>>>(notifyObjectModified)) {
            IConsumer<SchemaPath> ics = spi.get();
            if (ics == null) {
                notifyObjectModified.remove(spi);
            } else if (sp != null) {
                ics.accept(sp);
            }
        }
    }

    public int countModificationListeners(RubyIO p) {
        int n = 0;
        LinkedList<WeakReference<IConsumer<SchemaPath>>> notifyObjectModified = objectListenersMap.get(p);
        handleNotificationList(notifyObjectModified, null);
        if (notifyObjectModified != null)
            n += notifyObjectModified.size();
        String r = getIdByObject(p);
        if (r != null) {
            notifyObjectModified = objectRootListenersMap.get(r);
            handleNotificationList(notifyObjectModified, null);
            if (notifyObjectModified != null)
                n += notifyObjectModified.size();
        }
        return n;
    }

    private void removeFromGOCMH(LinkedList<WeakReference<IConsumer<SchemaPath>>> orCreateModificationHandlers, IConsumer<SchemaPath> handler) {
        WeakReference wr = null;
        for (WeakReference<IConsumer<SchemaPath>> w : orCreateModificationHandlers) {
            if (w.get() == handler) {
                wr = w;
                break;
            }
        }
        if (wr != null)
            orCreateModificationHandlers.remove(wr);
    }

    public void ensureAllSaved() {
        for (RubyIO rio : new LinkedList<RubyIO>(modifiedObjects)) {
            String id = getIdByObject(rio);
            if (id != null)
                ensureSaved(id, rio);
        }
    }
}
