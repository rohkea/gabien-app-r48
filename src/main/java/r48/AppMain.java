/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48;

import gabien.GaBIEn;
import gabien.IGrDriver;
import gabien.IGrInDriver;
import gabien.IImage;
import gabien.ui.*;
import gabienapp.Application;
import r48.dbs.*;
import r48.imagefx.ImageFXCache;
import r48.io.IObjectBackend;
import r48.map.StuffRenderer;
import r48.map.UIMapView;
import r48.map.systems.*;
import r48.schema.OpaqueSchemaElement;
import r48.schema.util.ISchemaHost;
import r48.schema.util.SchemaHostImpl;
import r48.schema.util.SchemaPath;
import r48.toolsets.*;
import r48.ui.Art;
import r48.ui.Coco;
import r48.ui.UIAppendButton;
import r48.ui.UINSVertLayout;
import r48.ui.help.HelpSystemController;
import r48.ui.help.UIHelpSystem;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

/**
 * Pre-release development notice. 31 Dec, 2016.
 * I'll finish some commands before releasing, but this is still going to be released a bit early.
 * Several schemas are missing. I guess it's okay enough that the schemas that do exist, well, exist...
 * ... but it would be nice if everything was in place. Oh well.
 * At least something good will come out of this year.
 * I've added the original Inspector (UITest) as a launchable thing so that examining data to write new schemas is possible.
 * Hopefully the system is flexible enough to support everything now, at least more or less.
 * In any case, if you're reading this you're examining the code.
 * This class holds the static members for several critical databases,
 * needed to keep the system running.
 * So, uh, don't lose it.
 * <p/>
 * -- NOTE: This is a 2017 version of the code,
 * since I decided to actually finish it.
 * If I do get around to releasing it,
 * well, you'll find the new features yourself,
 * I'm sure of it. --
 * <p/>
 * Created on 12/27/16.
 */
public class AppMain {
    // Where new windows go
    private static IConsumer<UIElement> windowMaker;

    // Scheduled tasks
    public static HashSet<Runnable> pendingRunnables = new HashSet<Runnable>();

    //private static UILabel uiStatusLabel;

    public static String rootPath = null;
    public static String dataPath = "";
    public static String dataExt = "";
    public static String odbBackend = "<you forgot to select a backend>";
    // Null system backend will always "work"
    public static String sysBackend = "null";

    // Databases
    public static ObjectDB objectDB = null;
    public static ATDB[] autoTiles = new ATDB[0];
    public static SDB schemas = null;

    // Backend Services

    // The global context-independent stuffRenderer. *Only use outside of maps.*
    public static StuffRenderer stuffRendererIndependent;
    public static MapSystem system;

    // ONLY this class should refer to this (I think?)
    private static IMapContext mapContext;
    private static UIWindowView rootView;
    private static IConsumer<UIElement> insertTab, insertImmortalTab;

    // NOTE: These two are never cleaned up and do not carry baggage
    private static IConsumer<UIElement> rootViewWM = new IConsumer<UIElement>() {
        @Override
        public void accept(final UIElement uiElement) {
            rootView.accept(new UIWindowView.WVWindow(uiElement, new UIWindowView.IWVWindowIcon[] {
                    new UIWindowView.IWVWindowIcon() {
                        @Override
                        public void draw(IGrDriver igd, int x, int y, int size) {
                            igd.clearRect(128, 64, 64, x, y, size, size);
                            if (uiElement instanceof IWindowElement)
                                if (((IWindowElement) uiElement).wantsSelfClose()) {
                                    rootView.removeByUIE(uiElement);
                                    ((IWindowElement) uiElement).windowClosed();
                                }
                        }

                        @Override
                        public void click() {
                            if (uiElement instanceof IWindowElement)
                                ((IWindowElement) uiElement).windowClosed();
                            rootView.removeByUIE(uiElement);
                        }
                    },
                    new UIWindowView.IWVWindowIcon() {
                        @Override
                        public void draw(IGrDriver igd, int x, int y, int size) {
                            Art.tabWindowIcon(igd, x, y, size);
                        }

                        @Override
                        public void click() {
                            rootView.removeByUIE(uiElement);
                            insertTab.accept(uiElement);
                        }
                    }
            }));
        }
    };
    private static IConsumer<UIElement> rootViewWMI = new IConsumer<UIElement>() {
        @Override
        public void accept(final UIElement uiElement) {
            rootView.accept(new UIWindowView.WVWindow(uiElement, new UIWindowView.IWVWindowIcon[] {
                    new UIWindowView.IWVWindowIcon() {
                        @Override
                        public void draw(IGrDriver igd, int x, int y, int size) {
                            Art.tabWindowIcon(igd, x, y, size);
                        }

                        @Override
                        public void click() {
                            rootView.removeByUIE(uiElement);
                            insertImmortalTab.accept(uiElement);
                        }
                    }
            }));
        }
    };

    // State for in-system copy/paste
    public static RubyIO theClipboard = null;

    // Images
    public static IImage layerTabs = GaBIEn.getImageCK("layertab.png", 0, 0, 0);
    public static IImage noMap = GaBIEn.getImageCK("nomad.png", 0, 0, 0);
    public static ImageFXCache imageFXCache = null;

    // All active schema hosts
    private static LinkedList<ISchemaHost> activeHosts;

    // -- For one schema element only --
    public static HashMap<Integer, String> osSHESEDB;

    public static IConsumer<Double> initializeAndRun(final String rp, final String gamepak, final IConsumer<UIElement> uiTicker) throws IOException {
        rootPath = rp;
        // initialize core resources

        schemas = new SDB();

        schemas.readFile(gamepak + "Schema.txt"); // This does a lot of IO, for one line.

        // initialize everything else that needs initializing, starting with ObjectDB

        objectDB = new ObjectDB(IObjectBackend.Factory.create(odbBackend, rootPath, dataPath, dataExt));

        if (sysBackend.equals("null")) {
            system = new NullSystem();
        } else if (sysBackend.equals("RXP")) {
            system = new RXPSystem();
        } else if (sysBackend.equals("RVXA")) {
            system = new RVXASystem();
        } else if (sysBackend.equals("Ika")) {
            system = new IkaSystem();
        } else if (sysBackend.equals("R2k")) {
            system = new R2kSystem();
        } else {
            throw new IOException("Unknown MapSystem backend " + sysBackend);
        }

        // Final internal consistency checks and reading in dictionaries from target
        //  before starting the UI, which can cause external consistency checks
        //  (...and potentially cause havoc in the process)

        schemas.startupSanitizeDictionaries(); // in case an object using dictionaries has to be created to use dictionaries
        schemas.updateDictionaries(null);
        schemas.confirmAllExpectationsMet();

        // Initialize imageFX before doing anything graphical
        imageFXCache = new ImageFXCache();

        activeHosts = new LinkedList<ISchemaHost>();

        // initialize UI
        rootView = new UIWindowView() {
            @Override
            public void updateAndRender(int ox, int oy, double deltaTime, boolean selected, IGrInDriver igd) {
                Coco.run(igd);
                super.updateAndRender(ox, oy, deltaTime, selected, igd);
            }
        };
        rootView.windowTextHeight = FontSizes.windowFrameHeight;
        rootView.sizerSize = rootView.windowTextHeight * 2;
        rootView.sizerOfs = (rootView.windowTextHeight * 4) / 3;
        windowMaker = rootViewWM;
        rootView.setBounds(new Rect(0, 0, 800, 600));

        // Set up a default stuffRenderer for things to use.
        stuffRendererIndependent = system.rendererFromTso(null);

        final UILabel uiStatusLabel = rebuildInnerUI(gamepak, uiTicker);

        // everything ready, start main window
        uiTicker.accept(rootView);

        return new IConsumer<Double>() {
            @Override
            public void accept(Double deltaTime) {
                // Why throw the full format syntax parser on this? Consistency, plus I can extend this format further if need be.

                uiStatusLabel.Text = FormatSyntax.formatExtended(TXDB.get("#A modified. Clipboard: #B"), new RubyIO().setFX(objectDB.modifiedObjects.size()), (theClipboard == null) ? new RubyIO().setNull() : theClipboard);
                if (mapContext != null) {
                    String mapId = mapContext.getCurrentMapObject();
                    RubyIO map = null;
                    if (mapId != null)
                        map = objectDB.getObject(mapId);
                    schemas.updateDictionaries(map);
                } else {
                    schemas.updateDictionaries(null);
                }

                LinkedList<Runnable> runs = new LinkedList<Runnable>(pendingRunnables);
                pendingRunnables.clear();
                for (Runnable r : runs)
                    r.run();

                LinkedList<ISchemaHost> newActive = new LinkedList<ISchemaHost>();
                for (ISchemaHost ac : activeHosts)
                    if (ac.isActive())
                        newActive.add(ac);
                activeHosts = newActive;
            }
        };
    }

    // This can only be done once now that rootView & the tab pane kind of share state.
    // For a proper UI reset, careful nuking is required.
    private static UITabPane initializeTabs(final String gamepak, final IConsumer<UIElement> uiTicker) {

        ISupplier<IConsumer<UIElement>> wmg = new ISupplier<IConsumer<UIElement>>() {
            @Override
            public IConsumer<UIElement> get() {
                return windowMaker;
            }
        };

        LinkedList<IToolset> toolsets = new LinkedList<IToolset>();
        if (system.enableMapSubsystem) {
            MapToolset mapController = new MapToolset(wmg);
            // Really just restricts access to prevent a hax pileup
            mapContext = mapController.getContext();
            toolsets.add(mapController);
        } else {
            mapContext = null;
        }
        if (system instanceof IRMMapSystem)
            toolsets.add(new RMToolsToolset(gamepak));
        toolsets.add(new BasicToolset(rootViewWM, uiTicker, new IConsumer<IConsumer<UIElement>>() {
            @Override
            public void accept(IConsumer<UIElement> uiElementIConsumer) {
                windowMaker = uiElementIConsumer;
            }
        }));
        toolsets.add(new ImageEditToolset());

        final UITabPane utp = new UITabPane(FontSizes.tabTextHeight, true);
        Runnable runVisFrame = new Runnable() {
            @Override
            public void run() {
                double keys = objectDB.objectMap.keySet().size();
                if (keys < 1) {
                    utp.visualizationOrange = 0.0d;
                } else {
                    utp.visualizationOrange = objectDB.modifiedObjects.size() / keys;
                }
                pendingRunnables.add(this);
            }
        };
        pendingRunnables.add(runVisFrame);
        insertImmortalTab = new IConsumer<UIElement>() {
            @Override
            public void accept(final UIElement uiElement) {
                utp.addTab(new UIWindowView.WVWindow(uiElement, new UIWindowView.IWVWindowIcon[] {
                        new UIWindowView.IWVWindowIcon() {
                            @Override
                            public void draw(IGrDriver igd, int x, int y, int size) {
                                Art.windowWindowIcon(igd, x, y, size);
                            }

                            @Override
                            public void click() {
                                utp.removeTab(uiElement);
                                Rect r = rootView.getBounds();
                                uiElement.setBounds(new Rect(0, 0, r.width / 2, r.height / 2));
                                rootViewWMI.accept(uiElement);
                            }
                        }
                }));
            }
        };

        // Initialize toolsets.
        for (IToolset its : toolsets)
            for (UIElement uie : its.generateTabs(wmg))
                insertImmortalTab.accept(uie);

        insertTab = new IConsumer<UIElement>() {
            @Override
            public void accept(final UIElement uiElement) {
                utp.addTab(new UIWindowView.WVWindow(uiElement, new UIWindowView.IWVWindowIcon[] {
                        new UIWindowView.IWVWindowIcon() {
                            @Override
                            public void draw(IGrDriver igd, int x, int y, int size) {
                                igd.clearRect(128, 64, 64, x, y, size, size);
                                if (uiElement instanceof IWindowElement)
                                    if (((IWindowElement) uiElement).wantsSelfClose()) {
                                        utp.removeTab(uiElement);
                                        ((IWindowElement) uiElement).windowClosed();
                                    }
                            }

                            @Override
                            public void click() {
                                if (uiElement instanceof IWindowElement)
                                    ((IWindowElement) uiElement).windowClosed();
                                utp.removeTab(uiElement);
                            }
                        },
                        new UIWindowView.IWVWindowIcon() {
                            @Override
                            public void draw(IGrDriver igd, int x, int y, int size) {
                                Art.windowWindowIcon(igd, x, y, size);
                            }

                            @Override
                            public void click() {
                                utp.removeTab(uiElement);
                                Rect r = rootView.getBounds();
                                uiElement.setBounds(new Rect(0, 0, r.width / 2, r.height / 2));
                                rootViewWM.accept(uiElement);
                            }
                        }
                }));
            }
        };
        return utp;
    }

    private static UILabel rebuildInnerUI(final String gamepak, final IConsumer<UIElement> uiTicker) {
        UILabel uiStatusLabel = new UILabel(TXDB.get("Loading..."), FontSizes.statusBarTextHeight);

        UIAppendButton workspace = new UIAppendButton(TXDB.get("Save All Modified Files"), uiStatusLabel, new Runnable() {
            @Override
            public void run() {
                objectDB.ensureAllSaved();
            }
        }, FontSizes.statusBarTextHeight);
        workspace = new UIAppendButton(TXDB.get("Clipboard"), workspace, new Runnable() {
            @Override
            public void run() {
                windowMaker.accept(new UIAutoclosingPopupMenu(new String[] {
                        TXDB.get("Save Clipboard To 'clip.r48'"),
                        TXDB.get("Load Clipboard From 'clip.r48'"),
                        TXDB.get("Inspect Clipboard"),
                        TXDB.get("Execute Lua from 'script.lua' onto clipboard")
                }, new Runnable[] {
                        new Runnable() {
                            @Override
                            public void run() {
                                if (theClipboard == null) {
                                    launchDialog(TXDB.get("There is nothing in the clipboard."));
                                } else {
                                    AdHocSaveLoad.save("clip", theClipboard);
                                    launchDialog(TXDB.get("The clipboard was saved."));
                                }
                            }
                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                RubyIO newClip = AdHocSaveLoad.load("clip");
                                if (newClip == null) {
                                    launchDialog(TXDB.get("The clipboard file is invalid or does not exist."));
                                } else {
                                    theClipboard = newClip;
                                    launchDialog(TXDB.get("The clipboard file was loaded."));
                                }
                            }
                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                if (theClipboard == null) {
                                    launchDialog(TXDB.get("There is nothing in the clipboard."));
                                } else {
                                    windowMaker.accept(new UITest(theClipboard));
                                }
                            }
                        },
                        new Runnable() {
                            @Override
                            public void run() {
                                if (theClipboard == null) {
                                    launchDialog(TXDB.get("There is nothing in the clipboard."));
                                } else {
                                    if (!LuaInterface.luaAvailable()) {
                                        launchDialog(TXDB.get("Lua isn't installed, so can't use it."));
                                    } else {
                                        try {
                                            BufferedReader br = new BufferedReader(new InputStreamReader(GaBIEn.getFile("script.lua"), "UTF-8"));
                                            String t = "";
                                            while (br.ready())
                                                t += br.readLine() + "\r\n";
                                            br.close();
                                            RubyIO rio = LuaInterface.runLuaCall(theClipboard, t);
                                            if (rio == null) {
                                                String s = "";
                                                try {
                                                    if (LuaInterface.lastError != null)
                                                        s = "\n" + new String(LuaInterface.lastError, "UTF-8");
                                                } catch (Exception e2) {
                                                    // output clearly unavailable
                                                }
                                                launchDialog(TXDB.get("Lua error, or took > 10 seconds. Output:") + s);
                                            } else {
                                                theClipboard = rio;
                                                launchDialog(TXDB.get("Successful - the clipboard was replaced."));
                                            }
                                        } catch (Exception e) {
                                            launchDialog(TXDB.get("An exception occurred? (R48-core files are stored in R48's current directory, not the root path.)"));
                                        }
                                    }
                                }
                            }
                        }
                }, FontSizes.menuTextHeight, true));
            }
        }, FontSizes.statusBarTextHeight);
        workspace = new UIAppendButton(TXDB.get("Help"), workspace, new Runnable() {
            @Override
            public void run() {
                startHelp(0);
            }
        }, FontSizes.statusBarTextHeight);
        rootView.backing = new UINSVertLayout(workspace, initializeTabs(gamepak, uiTicker));
        return uiStatusLabel;
    }

    // Notably, you can't use this for non-roots because you'll end up bypassing ObjectDB.
    public static ISchemaHost launchSchema(String s, RubyIO rio, UIMapView context) {
        // Responsible for keeping listeners in place so nothing breaks.
        SchemaHostImpl watcher = new SchemaHostImpl(windowMaker, context);
        watcher.switchObject(new SchemaPath(schemas.getSDBEntry(s), rio));
        return watcher;
    }

    public static ISchemaHost launchNonRootSchema(RubyIO root, String rootSchema, RubyIO arrayIndex, RubyIO element, String elementSchema, String indexText, UIMapView context) {
        // produce a valid (and false) parent chain, that handles all required guarantees.
        ISchemaHost shi = launchSchema(rootSchema, root, context);
        SchemaPath sp = new SchemaPath(AppMain.schemas.getSDBEntry(rootSchema), root);
        sp = sp.arrayHashIndex(arrayIndex, indexText);
        shi.switchObject(sp.newWindow(AppMain.schemas.getSDBEntry(elementSchema), element));
        return shi;
    }

    public static void launchDialog(String s) {
        UIHelpSystem uhs = new UIHelpSystem();
        for (String st : s.split("\n"))
            uhs.page.add(new UIHelpSystem.HelpElement('.', st.split(" ")));
        UIScrollLayout svl = new UIScrollLayout(true, FontSizes.generalScrollersize) {
            @Override
            public String toString() {
                return TXDB.get("Information");
            }
        };
        svl.panels.add(uhs);
        uhs.setBounds(uhs.getBounds());
        int h = uhs.getBounds().height;
        int limit = rootView.getBounds().height - rootView.getWindowFrameHeight();
        limit *= 3;
        limit /= 4;
        if (h > limit)
            h = limit;
        svl.setBounds(new Rect(0, 0, uhs.getBounds().width, h));
        windowMaker.accept(svl);
    }

    public static void startHelp(Integer integer) {
        // exception to the rule
        UILabel uil = new UILabel("", FontSizes.helpPathHeight);
        final UIHelpSystem uis = new UIHelpSystem();
        final HelpSystemController hsc = new HelpSystemController(uil, null, uis);
        uis.onLinkClick = new IConsumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                hsc.loadPage(integer);
            }
        };
        final UIScrollLayout uus = new UIScrollLayout(true, FontSizes.generalScrollersize);
        uus.panels.add(uis);
        uus.setBounds(new Rect(0, 0, (rootView.getBounds().width / 3) * 2, rootView.getBounds().height / 2));
        final UINSVertLayout topbar = new UINSVertLayout(new UIAppendButton(TXDB.get("Index"), uil, new Runnable() {
            @Override
            public void run() {
                hsc.loadPage(0);
            }
        }, FontSizes.helpPathHeight), uus) {
            @Override
            public String toString() {
                return TXDB.get("Help Window");
            }
        };
        hsc.onLoad = new Runnable() {
            @Override
            public void run() {
                uus.scrollbar.scrollPoint = 0;
                Rect b = topbar.getBounds();
                topbar.setBounds(b);
            }
        };
        hsc.loadPage(integer);
        windowMaker.accept(topbar);
    }

    // R2kSystemDefaultsInstallerSchemaElement uses this to indirectly access several things a SchemaElement isn't allowed to access.
    public static void r2kProjectCreationHelperFunction() {
        Runnable deploy = new Runnable() {
            @Override
            public void run() {
                // Perform all mkdirs
                String[] mkdirs = {
                        "Backdrop",
                        "Battle",
                        "Battle2",
                        "BattleCharSet",
                        "BattleWeapon",
                        "CharSet",
                        "ChipSet",
                        "FaceSet",
                        "Frame",
                        "GameOver",
                        "Monster",
                        "Music",
                        "Panorama",
                        "Picture",
                        "Sound",
                        "System",
                        "System2",
                        "Title"
                };
                String[] fileCopies = {
                        "R2K/char.png", "CharSet/char.png",
                        "R2K/System.png", "System/System.png",
                        "R2K/templatetileset.png", "ChipSet/templatetileset.png",
                        "R2K/slime.png", "Monster/monster.png",
                };
                for (String s : mkdirs)
                    GaBIEn.makeDirectories(AppMain.rootPath + s);
                for (int i = 0; i < fileCopies.length; i += 2) {
                    String src = fileCopies[i];
                    String dst = fileCopies[i + 1];
                    InputStream inp = GaBIEn.getResource(src);
                    if (inp != null) {
                        OutputStream oup = GaBIEn.getOutFile(rootPath + dst);
                        if (oup != null) {
                            try {
                                byte[] b = new byte[2048];
                                while (inp.available() > 0)
                                    oup.write(b, 0, inp.read(b));
                            } catch (IOException ioe) {

                            }
                            try {
                                oup.close();
                            } catch (IOException ioe) {

                            }
                        }
                        try {
                            inp.close();
                        } catch (IOException ioe) {
                        }
                    }
                }
                // Load map 1, save everything
                mapContext.loadMap("Map.1");
                objectDB.ensureAllSaved();
                launchDialog(TXDB.get("2k3 template synthesis complete."));
            }
        };
        windowMaker.accept(new UIAutoclosingPopupMenu(new String[] {
                TXDB.get("You are creating a RPG Maker 2000/2003 LDB."),
                TXDB.get("Click here to automatically build skeleton project."),
                TXDB.get("Otherwise, close this inner window."),
        }, new Runnable[] {
                deploy,
                deploy,
                deploy
        }, FontSizes.menuTextHeight, true));
    }

    public static void pleaseShutdown() {
        Application.shutdownAllAppMainWindows();
    }

    public static void shutdown() {
        windowMaker = null;
        pendingRunnables.clear();
        rootPath = null;
        dataPath = "";
        dataExt = "";
        odbBackend = "<you forgot to select a backend>";
        sysBackend = "null";
        objectDB = null;
        autoTiles = new ATDB[0];
        schemas = null;
        stuffRendererIndependent = null;
        system = null;
        if (mapContext != null)
            mapContext.freeOsbResources();
        mapContext = null;
        rootView = null;
        insertImmortalTab = null;
        insertTab = null;
        theClipboard = null;
        imageFXCache = null;
        activeHosts = null;
        osSHESEDB = null;
        TXDB.flushNameDB();
        GaBIEn.hintFlushAllTheCaches();
    }

    // Used for event selection boxes.
    public static boolean currentlyOpenInEditor(RubyIO r) {
        for (ISchemaHost ish : activeHosts) {
            SchemaPath sp = ish.getCurrentObject();
            while (sp != null) {
                if (sp.targetElement == r)
                    return true;
                sp = sp.parent;
            }
        }
        return false;
    }

    public static void schemaHostImplRegister(SchemaHostImpl shi) {
        activeHosts.add(shi);
    }

    // Is this messy? Yes. Is it required? After someone lost some work to R48? YES IT DEFINITELY IS.
    // Later: I've reduced the amount of backups performed because it appears spikes were occurring all the time.
    public static void performSystemDump(boolean emergency) {
        RubyIO n = new RubyIO();
        n.setHash();
        for (RubyIO rio : objectDB.modifiedObjects) {
            String s = objectDB.getIdByObject(rio);
            if (s != null)
                n.hashVal.put(new RubyIO().setString(s, true), rio);
        }
        if (!emergency) {
            RubyIO n2 = new RubyIO();
            n2.setString(TXDB.get("R48 Non-Emergency Backup File. This file can be used in place of r48.error.YOUR_SAVED_DATA.r48 in case of power failure or corrupting error. Assuming you actually save often it won't get too big - otherwise you need the reliability."), true);
            RubyIO n3 = AdHocSaveLoad.load("r48.pfail.YOUR_SAVED_DATA");
            if (n3 != null) {
                // Unlink for disk space & memory usage reasons.
                // Already this is going to eat RAM.
                n3.rmIVar("@last");
                n2.addIVar("@last", n3);
            }
            n2.addIVar("@current", n);
            n = n2;
        }
        if (emergency)
            System.err.println("emergency dump is now actually occurring. Good luck.");
        AdHocSaveLoad.save(emergency ? "r48.error.YOUR_SAVED_DATA" : "r48.pfail.YOUR_SAVED_DATA", n);
        if (emergency)
            System.err.println("emergency dump is complete.");
    }

    public static void reloadSystemDump() {
        RubyIO sysDump = AdHocSaveLoad.load("r48.error.YOUR_SAVED_DATA");
        if (sysDump == null) {
            AppMain.launchDialog("The system dump was unloadable. It should be r48.error.YOUR_SAVED_DATA.r48");
            return;
        }
        RubyIO possibleActualDump = sysDump.getInstVarBySymbol("@current");
        if (possibleActualDump != null)
            sysDump = possibleActualDump;
        for (Map.Entry<RubyIO, RubyIO> rio : sysDump.hashVal.entrySet()) {
            String name = rio.getKey().decString();
            RubyIO root = objectDB.getObject(name);
            if (root == null) {
                root = new RubyIO();
                root.setNull();
                objectDB.newlyCreatedObjects.add(root);
                objectDB.objectMap.put(name, new WeakReference<RubyIO>(root));
            }
            root.setDeepClone(rio.getValue());
            objectDB.objectRootModified(root, new SchemaPath(new OpaqueSchemaElement(), root));
        }
        if (possibleActualDump != null) {
            AppMain.launchDialog("Power failure dump loaded.");
        } else {
            AppMain.launchDialog("Error dump loaded.");
        }
    }

    // Attempts to ascertain all known objects
    public static LinkedList<String> getAllObjects() {
        // anything loaded gets added (this allows some bypass of the mechanism)
        HashSet<String> mainSet = new HashSet<String>(objectDB.objectMap.keySet());
        mainSet.addAll(schemas.listFileDefs());
        if (system instanceof IRMMapSystem) {
            IRMMapSystem rms = (IRMMapSystem) system;
            for (IRMMapSystem.RMMapData rio : rms.getAllMaps())
                mainSet.add(rio.idName);
        }
        return new LinkedList<String>(mainSet);
    }
}
