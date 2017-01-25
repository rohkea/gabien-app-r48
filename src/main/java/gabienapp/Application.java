/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabienapp;

import gabien.GaBIEn;
import gabien.IGrInDriver;
import gabien.ui.*;
import gabienapp.dbs.*;
import gabienapp.map.UIMapView;
import gabienapp.map.UIMapViewContainer;
import gabienapp.maptools.UIMTEventPicker;
import gabienapp.musicality.Musicality;
import gabienapp.schema.ISchemaElement;
import gabienapp.schema.util.ISchemaHost;
import gabienapp.schema.util.SchemaHostImpl;
import gabienapp.schema.util.SchemaPath;
import gabienapp.ui.*;

import java.io.*;
import java.util.Iterator;
import java.util.LinkedList;

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
 *  needed to keep the system running.
 * So, uh, don't lose it.
 *
 * -- NOTE: This is a 2017 version of the code,
 *          since I decided to actually finish it.
 *          If I do get around to releasing it,
 *           well, you'll find the new features yourself,
 *           I'm sure of it. --
 *
 * Created on 12/27/16.
 */
public class Application {
    // Where new windows go
    private static IConsumer<UIElement> windowMaker;

    // mapBox creates the stuffRenderer,
    //  and lots of rendering logic is there rather than in mapBox,
    //  so it's easy enough to pass a tileset and give yourself an alternative rendering context.
    // Or, as I'm intending, have the ISchemaHost hold the StuffRenderer from the correct context.
    private static UIMapViewContainer mapBox;
    public static StuffRenderer stuffRenderer;

    public static UIElement nextMapTool = null;

    public static String rootPath = null;
    public static String dataPath = "";
    public static ObjectDB objectDB = null;
    // rootPath must be above the others
    // 053: Cafe
    public static RubyIO tilesets = null;

    // Databases
    public static ATDB autoTiles = null;
    public static SDB schemas = null;

    public static RubyIO theClipboard = null;

    // Images
    public static IGrInDriver.IImage layerTabs = GaBIEn.getImage("layertab.png", 0, 0, 0);

    public static void gabienmain() throws IOException {
        rootPath = "";

        // initialize core resources

        schemas = new SDB();

        InputStreamReader fr = new InputStreamReader(GaBIEn.getFile("Schema.txt"));
        schemas.readFile(new BufferedReader(fr));
        fr.close();

        // initialize everything else that needs initializing, starting with ObjectDB

        objectDB = new ObjectDB(rootPath + dataPath, ".rxdata");

        // Final internal consistency checks and reading in dictionaries from target
        //  before starting the UI, which can cause external consistency checks
        //  (...and potentially cause havoc in the process)

        schemas.updateDictionaries();
        schemas.confirmAllExpectationsMet();

        tilesets = objectDB.getObject("Tilesets");

        // initialize UI
        final WindowCreatingUIElementConsumer uiTicker = new WindowCreatingUIElementConsumer();
        final UIWindowView rootView = new UIWindowView();
        windowMaker = rootView;
        rootView.setBounds(new Rect(0, 0, 640, 480));

        // Set up a default stuffRenderer for things to use.
        stuffRenderer = new StuffRenderer(null);

        ISupplier<IConsumer<UIElement>> wmg = new ISupplier<IConsumer<UIElement>>() {
            @Override
            public IConsumer<UIElement> get() {
                return windowMaker;
            }
        };
        LinkedList<String> tabNames = new LinkedList<String>();
        LinkedList<UIElement> tabElems = new LinkedList<UIElement>();

        // Try to find out if this is a glorified sticky note editor,
        //  or if it's been configured correctly.
        try {

            // Note: ensure the things a tab depends on exist and won't error before the tab itself is put in.
            // At the first exception, this bails out.
            mapBox = new UIMapViewContainer(wmg);
            UIMapInfos mapInfoEl = new UIMapInfos(wmg);
            tabNames.add("Map");
            tabElems.add(mapBox);
            tabNames.add("MapInfos");
            tabElems.add(mapInfoEl);

            final CMDB commandsEvent = schemas.getCMDB("RXP/Commands.txt");
            final ISchemaElement commandEvent = schemas.getSDBEntry("EventCommandEditor");

            tabNames.add("Tools");
            tabElems.add(new UIPopupMenu(new String[]{
                    "Locate EventCommand in all Pages",
                    "See If Autocorrect Modifies Anything",
                    // 3:24 PM, third day of 2017.
                    // This is now a viable option.
                    // 3:37 PM, same day.
                    // All EventCommands found in the maps of the test subject seem completed.
                    // Still need to see to the CommonEvents.
                    // next day, um, these tools aren't really doable post-further-modularization (stickynote)
                    // 5th January 2017. Here we go.
                    "Locate incomplete ECs",
                    "Locate incomplete ECs (CommonEvents)",
                    "Run EC Creation Sanity Check",
            }, new Runnable[]{
                    new Runnable() {
                        @Override
                        public void run() {
                            windowMaker.accept(new UITextPrompt("Code?", new IConsumer<String>() {
                                @Override
                                public void accept(String s) {
                                    int i = Integer.parseInt(s);
                                    for (RubyIO rio : objectDB.getObject("MapInfos").hashVal.keySet()) {
                                        RubyIO map = objectDB.getObject(UIMapView.getMapName((int) rio.fixnumVal));
                                        // Find event!
                                        for (RubyIO event : map.getInstVarBySymbol("@events").hashVal.values()) {
                                            for (RubyIO page : event.getInstVarBySymbol("@pages").arrVal) {
                                                for (RubyIO cmd : page.getInstVarBySymbol("@list").arrVal) {
                                                    if (cmd.getInstVarBySymbol("@code").fixnumVal == i) {
                                                        UIMTEventPicker.showEvent(event.getInstVarBySymbol("@id").fixnumVal, map, event);
                                                        return;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    windowMaker.accept(new UILabel("nnnope", true));
                                }
                            }));
                        }
                    },
                    new Runnable() {
                        @Override
                        public void run() {
                            LinkedList<String> objects = new LinkedList<String>();
                            LinkedList<String> objectSchemas = new LinkedList<String>();
                            for (String s : schemas.listFileDefs()) {
                                objects.add(s);
                                objectSchemas.add("File." + s);
                            }
                            for (RubyIO rio : objectDB.getObject("MapInfos").hashVal.keySet()) {
                                objects.add(UIMapView.getMapName((int) rio.fixnumVal));
                                objectSchemas.add("RPG::Map");
                            }
                            Iterator<String> schemaIt = objectSchemas.iterator();
                            for (final String obj : objects) {
                                RubyIO map = objectDB.getObject(obj);
                                Runnable modListen = new Runnable() {
                                    @Override
                                    public void run() {
                                        // yup, and throw an exception to give the user an idea of the tree
                                        throw new RuntimeException("MODIFY" + obj);
                                    }
                                };
                                objectDB.registerModificationHandler(map, modListen);
                                SchemaPath sp = new SchemaPath(Application.schemas.getSDBEntry(schemaIt.next()), map, null);
                                sp.editor.modifyVal(map, sp, false);
                                objectDB.deregisterModificationHandler(map, modListen);
                            }
                            windowMaker.accept(new UILabel("nnnope", true));
                        }
                    },
                    new Runnable() {
                        @Override
                        public void run() {
                            for (RubyIO rio : objectDB.getObject("MapInfos").hashVal.keySet()) {
                                RubyIO map = objectDB.getObject(UIMapView.getMapName((int) rio.fixnumVal));
                                // Find event!
                                for (RubyIO event : map.getInstVarBySymbol("@events").hashVal.values()) {
                                    for (RubyIO page : event.getInstVarBySymbol("@pages").arrVal) {
                                        for (RubyIO cmd : page.getInstVarBySymbol("@list").arrVal) {
                                            if (!commandsEvent.knownCommands.containsKey((int) cmd.getInstVarBySymbol("@code").fixnumVal)) {
                                                System.out.println(cmd.getInstVarBySymbol("@code").fixnumVal);
                                                UIMTEventPicker.showEvent(event.getInstVarBySymbol("@id").fixnumVal, map, event);
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                            windowMaker.accept(new UILabel("nnnope", true));
                        }
                    },
                    new Runnable() {
                        @Override
                        public void run() {
                            for (RubyIO rio : objectDB.getObject("CommonEvents").arrVal) {
                                if (rio.type != '0') {
                                    for (RubyIO cmd : rio.getInstVarBySymbol("@list").arrVal) {
                                        if (!commandsEvent.knownCommands.containsKey((int) cmd.getInstVarBySymbol("@code").fixnumVal)) {
                                            System.out.println(rio.getInstVarBySymbol("@id").fixnumVal);
                                            System.out.println(cmd.getInstVarBySymbol("@code").fixnumVal);
                                            windowMaker.accept(new UILabel("yup (chk.console)", true));
                                            return;
                                        }
                                    }
                                }
                            }
                            windowMaker.accept(new UILabel("nnnope", true));
                        }
                    },
                    new Runnable() {
                        @Override
                        public void run() {
                            // This won't deal with the really complicated disambiguator events, but it's something.
                            for (int cc : commandsEvent.knownCommands.keySet()) {
                                RubyIO r = SchemaPath.createDefaultValue(commandEvent, new RubyIO().setFX(0));
                                SchemaPath sp = new SchemaPath(commandEvent, r, null).arrayHashIndex(new RubyIO().setFX(0), "BLAH");
                                // this is bad, but it works well enough
                                r.getInstVarBySymbol("@code").fixnumVal = cc;
                                commandEvent.modifyVal(r, sp, false);
                            }
                        }
                    }
            }, true, false));
        } catch (Exception e) {
            System.err.println("Can't use MapInfos & such");
            e.printStackTrace();
        }
        tabNames.add("System Objects");
        tabElems.add(makeFileList());
        tabNames.add("ObjectDB Monitor");
        tabElems.add(new UIObjectDBMonitor());
        tabNames.add("System Tools");
        tabElems.add(new UIPopupMenu(new String[] {
                "Edit Object",
                "New Object via Schema, ODB'AnonObject'",
                "Inspect Object (no Schema needed)",
                "Set Internal Windows (good)",
                "Set External Windows (bad)",
                "Use normal in-built fonts",
                "Make larger text look better",
                "Make text N.I.Z.X.-compatible",
                "Toggle calming sound",
        }, new Runnable[]{
                new Runnable() {
                    @Override
                    public void run() {
                        windowMaker.accept(new UITextPrompt("Object Name?", new IConsumer<String>() {
                            @Override
                            public void accept(String s) {
                                final RubyIO rio = objectDB.getObject(s);
                                if (schemas.hasSDBEntry("File." + s)) {
                                    launchSchema("File." + s, rio);
                                    return;
                                }
                                if (rio != null) {
                                    if (rio.type == 'o') {
                                        if (rio.symVal != null) {
                                            if (schemas.hasSDBEntry(rio.symVal)) {
                                                launchSchema(rio.symVal, rio);
                                                return;
                                            }
                                        }
                                    }
                                    windowMaker.accept(new UITextPrompt("Schema ID?", new IConsumer<String>() {
                                        @Override
                                        public void accept(String s) {
                                            launchSchema(s, rio);
                                        }
                                    }));
                                } else {
                                    windowMaker.accept(new UILabel("No file, or schema to create it.", false));
                                }
                            }
                        }));
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        windowMaker.accept(new UITextPrompt("Schema ID?", new IConsumer<String>() {
                            @Override
                            public void accept(String s) {
                                launchSchema(s, SchemaPath.createDefaultValue(schemas.getSDBEntry(s), new RubyIO().setFX(0)));
                            }
                        }));
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        windowMaker.accept(new UITextPrompt("Object Name?", new IConsumer<String>() {
                            @Override
                            public void accept(String s) {
                                windowMaker.accept(new UITest(objectDB.getObject(s)));
                            }
                        }));
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        windowMaker = rootView;
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        windowMaker = uiTicker;
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        UILabel.iDislikeTheFont = false;
                        UILabel.iAmAbsolutelySureIHateTheFont = false;
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        UILabel.iDislikeTheFont = true;
                        UILabel.iAmAbsolutelySureIHateTheFont = false;
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        UILabel.iDislikeTheFont = true;
                        UILabel.iAmAbsolutelySureIHateTheFont = true;
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        if (!Musicality.initialized)
                            Musicality.initialize();
                        if (Musicality.running) {
                            Musicality.kill();
                        } else {
                            Musicality.boot();
                        }
                    }
                }
        }, false, false));
        UILabel uiStatusLabel = new UILabel("Loading...", false);
        rootView.backing = new UINSVertLayout(new UIHHalfsplit(5, 8, uiStatusLabel, new UIAppendButton("Help?", new UITextButton(false, "Save All Modified Files", new Runnable() {
            @Override
            public void run() {
                objectDB.ensureAllSaved();
            }
        }), new Runnable() {
            @Override
            public void run() {
                // exception to the rule
                UILabel uil = new UILabel("Blank Help Window", false);
                final UIHelpSystem uis = new UIHelpSystem(uil, null);
                final UIUnscissoredScroller uus = new UIUnscissoredScroller(uis);
                UINSVertLayout topbar = new UINSVertLayout(new UIAppendButton("Index", uil, new Runnable() {
                    @Override
                    public void run() {
                        uis.loadPage(0);
                    }
                }, false), uus);
                uis.onLoad = new Runnable() {
                    @Override
                    public void run() {
                        uis.setBounds(new Rect(0, 0, 640, 480));
                    }
                };
                uis.loadPage(0);
                uiTicker.accept(topbar);
            }
        }, false)), new UITabPane(tabNames.toArray(new String[0]), tabElems.toArray(new UIElement[0])));

        // everything ready, start main window
        uiTicker.accept(rootView);

        while (uiTicker.runningWindows() > 0) {
            uiStatusLabel.Text = objectDB.modifiedObjects.size() + " modified.";
            schemas.updateDictionaries();
            double dT = GaBIEn.timeDelta(false);
            while (dT < 0.02d) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                dT = GaBIEn.timeDelta(false);
            }
            dT = GaBIEn.timeDelta(true);
            if (Musicality.running)
                Musicality.update(dT);
            uiTicker.runTick(dT);
        }
        GaBIEn.ensureQuit();
    }

    private static UIElement makeFileList() {
        LinkedList<String> s = schemas.listFileDefs();
        LinkedList<Runnable> r = new LinkedList<Runnable>();
        for (final String s2 : s)
            r.add(new Runnable() {
                @Override
                public void run() {
                    launchSchema("File." + s2, objectDB.getObject(s2));
                }
            });
        return new UIPopupMenu(s.toArray(new String[0]), r.toArray(new Runnable[0]), true, false);
    }

    // this includes objects whose existence is defined by objects (maps)
    public static LinkedList<String> listAllObjects() {
        LinkedList<String> base = schemas.listFileDefs();
        for (RubyIO k : objectDB.getObject("MapInfos").hashVal.keySet())
            base.add(UIMapView.getMapName((int) k.fixnumVal));
        return base;
    }

    // Notably, you can't use this for non-roots because you'll end up bypassing ObjectDB.
    public static ISchemaHost launchSchema(String s, RubyIO rio) {
        // Responsible for keeping listeners in place so nothing breaks.
        SchemaHostImpl watcher = new SchemaHostImpl(windowMaker);
        watcher.switchObject(new SchemaPath(schemas.getSDBEntry(s), rio, watcher));
        return watcher;
    }

    public static ISchemaHost launchNonRootSchema(RubyIO root, String rootSchema, RubyIO array, String arraySchema, RubyIO arrayIndex, RubyIO element, String elementSchema, String indexText) {
        // produce a valid (and false) parent chain, that handles all required guarantees.
        ISchemaHost shi = launchSchema(rootSchema, root);
        SchemaPath sp = new SchemaPath(Application.schemas.getSDBEntry(rootSchema), root, shi);
        sp = sp.arrayEntry(array, Application.schemas.getSDBEntry(arraySchema));
        sp = sp.arrayHashIndex(arrayIndex, indexText);
        shi.switchObject(sp.newWindow(Application.schemas.getSDBEntry(elementSchema), element, shi));
        return shi;
    }

    public static void loadMap(int k) {
        mapBox.loadMap(k);
    }
}
