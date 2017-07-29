/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
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

    public ObjectDB(IObjectBackend b) {
        backend = b;
    }

    public HashMap<String, WeakReference<RubyIO>> objectMap = new HashMap<String, WeakReference<RubyIO>>();
    public WeakHashMap<RubyIO, String> reverseObjectMap = new WeakHashMap<RubyIO, String>();
    // The values don't actually matter -
    //  this locks the object into memory for as long as it's modified.
    public HashSet<RubyIO> modifiedObjects = new HashSet<RubyIO>();
    public HashSet<RubyIO> newlyCreatedObjects = new HashSet<RubyIO>();
    public WeakHashMap<RubyIO, LinkedList<IConsumer<SchemaPath>>> objectListenersMap = new WeakHashMap<RubyIO, LinkedList<IConsumer<SchemaPath>>>();

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
            if (!AppMain.schemas.hasSDBEntry(backupSchema)) {
                System.err.println("Could not find backup schema for object " + id);
                return null;
            }
            SchemaElement ise = AppMain.schemas.getSDBEntry(backupSchema);
            if (backupSchema != null) {
                if (ise != null) {
                    try {
                        rio = SchemaPath.createDefaultValue(ise, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                    modifiedObjects.add(rio);
                    newlyCreatedObjects.add(rio);
                } else {
                    return null;
                }
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
            AppMain.launchDialog(TXDB.get("Error: ") + ioe);
            ioe.printStackTrace();
        }
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

    private LinkedList<IConsumer<SchemaPath>> getOrCreateModificationHandlers(RubyIO p) {
        LinkedList<IConsumer<SchemaPath>> notifyObjectModified = objectListenersMap.get(p);
        if (notifyObjectModified == null) {
            notifyObjectModified = new LinkedList<IConsumer<SchemaPath>>();
            objectListenersMap.put(p, notifyObjectModified);
        }
        return notifyObjectModified;
    }

    // Note that these are run at the end of frame,
    //  because there appears to be a performance issue with these being spammed over and over again. Oops.

    public void registerModificationHandler(RubyIO root, IConsumer<SchemaPath> handler) {
        getOrCreateModificationHandlers(root).add(handler);
    }

    public void deregisterModificationHandler(RubyIO root, IConsumer<SchemaPath> handler) {
        getOrCreateModificationHandlers(root).remove(handler);
    }

    public void objectRootModified(RubyIO p, final SchemaPath path) {
        // Is this available in ObjectDB? If not, then it shouldn't be locked into permanent memory.
        // However, if there are modification listeners on this particular object, they get used
        if (reverseObjectMap.containsKey(p))
            modifiedObjects.add(p);
        LinkedList<IConsumer<SchemaPath>> notifyObjectModified = objectListenersMap.get(p);
        if (notifyObjectModified != null)
            for (final IConsumer<SchemaPath> r : new LinkedList<IConsumer<SchemaPath>>(notifyObjectModified))
                AppMain.pendingRunnables.add(new Runnable() {
                    @Override
                    public void run() {
                        r.accept(path);
                    }
                });
    }

    public int countModificationListeners(RubyIO p) {
        LinkedList<IConsumer<SchemaPath>> notifyObjectModified = objectListenersMap.get(p);
        if (notifyObjectModified != null)
            return notifyObjectModified.size();
        return 0;
    }

    public void ensureAllSaved() {
        for (RubyIO rio : new LinkedList<RubyIO>(modifiedObjects)) {
            String id = getIdByObject(rio);
            if (id != null)
                ensureSaved(id, rio);
        }
    }
}
