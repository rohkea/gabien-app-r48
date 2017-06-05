/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package r48;

import gabien.GaBIEn;
import gabien.IGrInDriver;
import gabien.ui.*;
import r48.dbs.ATDB;
import r48.dbs.ObjectDB;
import r48.dbs.SDB;
import r48.io.IkaObjectBackend;
import r48.io.R2kObjectBackend;
import r48.io.R48ObjectBackend;
import r48.map.StuffRenderer;
import r48.musicality.Musicality;
import r48.schema.SchemaElement;
import r48.schema.util.ISchemaHost;
import r48.schema.util.SchemaHostImpl;
import r48.schema.util.SchemaPath;
import r48.systems.*;
import r48.toolsets.BasicToolset;
import r48.toolsets.IToolset;
import r48.toolsets.MapToolset;
import r48.toolsets.RMToolsToolset;
import r48.ui.*;
import r48.ui.help.HelpSystemController;
import r48.ui.help.UIHelpSystem;

import java.io.IOException;
import java.util.HashSet;
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

    private static UILabel uiStatusLabel;
    private static MapToolset mapController;

    public static UIElement nextMapTool = null;

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

    // Backend Service (these are dealt with in StuffRenderer, since they're all really it's responsibility)

    public static StuffRenderer stuffRenderer;
    public static MapSystem system;

    // State for in-system copy/paste
    public static RubyIO theClipboard = null;

    // Images
    public static IGrInDriver.IImage layerTabs = GaBIEn.getImageCK("layertab.png", 0, 0, 0);
    public static IGrInDriver.IImage noMap = GaBIEn.getImageCK("nomad.png", 0, 0, 0);

    public static void initialize(String gamepack) throws IOException {
        rootPath = "";

        // initialize core resources

        schemas = new SDB();

        schemas.readFile(gamepack + "Schema.txt"); // This does a lot of IO, for one line.

        // initialize everything else that needs initializing, starting with ObjectDB

        if (odbBackend.equals("r48")) {
            objectDB = new ObjectDB(new R48ObjectBackend(rootPath + dataPath, dataExt, true));
        } else if (odbBackend.equals("ika")) {
            objectDB = new ObjectDB(new IkaObjectBackend(rootPath));
        } else if (odbBackend.equals("lcf2000")) {
            objectDB = new ObjectDB(new R2kObjectBackend(rootPath));
        } else {
            throw new IOException("Unknown ODB backend " + odbBackend);
        }

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

        schemas.updateDictionaries(null);
        schemas.confirmAllExpectationsMet();
    }

    public static IConsumer<Double> initializeAndRun(final IConsumer<UIElement> uiTicker) {

        // initialize UI
        final UIWindowView rootView = new UIWindowView();
        rootView.windowTextHeight = FontSizes.windowFrameHeight;
        windowMaker = rootView;
        rootView.setBounds(new Rect(0, 0, 800, 600));

        // Set up a default stuffRenderer for things to use.
        stuffRenderer = system.rendererFromMap(null);

        rebuildInnerUI(rootView, uiTicker);

        // everything ready, start main window
        uiTicker.accept(rootView);

        return new IConsumer<Double>() {
            @Override
            public void accept(Double deltaTime) {
                uiStatusLabel.Text = objectDB.modifiedObjects.size() + " modified.";
                if (mapController != null) {
                    schemas.updateDictionaries(mapController.getCurrentMap());
                } else {
                    schemas.updateDictionaries(null);
                }
                if (Musicality.running)
                    Musicality.update(deltaTime);

                LinkedList<Runnable> runs = new LinkedList<Runnable>();
                runs.addAll(pendingRunnables);
                pendingRunnables.clear();
                for (Runnable r : runs)
                    r.run();
            }
        };
    }

    private static UITabPane initializeTabs(final UIWindowView rootView, final IConsumer<UIElement> uiTicker) {
        LinkedList<String> tabNames = new LinkedList<String>();
        LinkedList<UIElement> tabElems = new LinkedList<UIElement>();

        LinkedList<IToolset> toolsets = new LinkedList<IToolset>();
        // Until a future time, this is hard-coded as the classname of a map being created via MapInfos.
        // Probably simple enough to create a special alias, but meh.
        if (AppMain.schemas.hasSDBEntry("RPG::Map")) {
            mapController = new MapToolset();
            toolsets.add(mapController);
        } else {
            mapController = null;
        }
        if (AppMain.schemas.hasSDBEntry("EventCommandEditor"))
            toolsets.add(new RMToolsToolset());
        toolsets.add(new BasicToolset(rootView, uiTicker, new IConsumer<IConsumer<UIElement>>() {
            @Override
            public void accept(IConsumer<UIElement> uiElementIConsumer) {
                windowMaker = uiElementIConsumer;
            }
        }, new Runnable() {
            @Override
            public void run() {
                rebuildInnerUI(rootView, uiTicker);
            }
        }));

        // Initialize toolsets.
        ISupplier<IConsumer<UIElement>> wmg = new ISupplier<IConsumer<UIElement>>() {
            @Override
            public IConsumer<UIElement> get() {
                return windowMaker;
            }
        };
        for (IToolset its : toolsets) {
            String[] tabs = its.tabNames();
            UIElement[] tabContents = its.generateTabs(wmg);
            // NOTE: This allows skipping out on actually generating tabs at the end, if you dare.
            for (int i = 0; i < tabContents.length; i++) {
                tabNames.add(tabs[i]);
                tabElems.add(tabContents[i]);
            }
        }

        return new UITabPane(tabNames.toArray(new String[0]), tabElems.toArray(new UIElement[0]), FontSizes.tabTextHeight);
    }

    private static void rebuildInnerUI(final UIWindowView rootView, final IConsumer<UIElement> uiTicker) {
        uiStatusLabel = new UILabel("Loading...", FontSizes.statusBarTextHeight);

        UIAppendButton workspace = new UIAppendButton("Save All Modified Files", uiStatusLabel, new Runnable() {
            @Override
            public void run() {
                objectDB.ensureAllSaved();
            }
        }, FontSizes.statusBarTextHeight);
        workspace = new UIAppendButton(" Help", workspace, new Runnable() {
            @Override
            public void run() {
                startHelp(0);
            }
        }, FontSizes.statusBarTextHeight);
        rootView.backing = new UINSVertLayout(workspace, initializeTabs(rootView, uiTicker));
    }

    // Notably, you can't use this for non-roots because you'll end up bypassing ObjectDB.
    public static ISchemaHost launchSchema(String s, RubyIO rio) {
        // Responsible for keeping listeners in place so nothing breaks.
        SchemaHostImpl watcher = new SchemaHostImpl(windowMaker);
        watcher.switchObject(new SchemaPath(schemas.getSDBEntry(s), rio, watcher));
        return watcher;
    }

    public static ISchemaHost launchNonRootSchema(RubyIO root, String rootSchema, RubyIO arrayIndex, RubyIO element, String elementSchema, String indexText) {
        // produce a valid (and false) parent chain, that handles all required guarantees.
        ISchemaHost shi = launchSchema(rootSchema, root);
        SchemaPath sp = new SchemaPath(AppMain.schemas.getSDBEntry(rootSchema), root, shi);
        sp = sp.arrayHashIndex(arrayIndex, indexText);
        shi.switchObject(sp.newWindow(AppMain.schemas.getSDBEntry(elementSchema), element, shi));
        return shi;
    }

    public static void launchDialog(String s) {
        windowMaker.accept(new UILabel(s, FontSizes.dialogWindowTextHeight));
    }

    public static void startHelp(Integer integer) {
        // exception to the rule
        UILabel uil = new UILabel("Blank Help Window", FontSizes.helpPathHeight);
        final UIHelpSystem uis = new UIHelpSystem();
        final HelpSystemController hsc = new HelpSystemController(uil, null, uis);
        uis.onLinkClick = new IConsumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                hsc.loadPage(integer);
            }
        };
        final UIScrollVertLayout uus = new UIScrollVertLayout();
        uus.panels.add(uis);
        uus.setBounds(new Rect(0, 0, 560, 240));
        final UINSVertLayout topbar = new UINSVertLayout(new UIAppendButton("Index", uil, new Runnable() {
            @Override
            public void run() {
                hsc.loadPage(0);
            }
        }, FontSizes.helpPathHeight), uus);
        hsc.onLoad = new Runnable() {
            @Override
            public void run() {
                uus.scrollbar.scrollPoint = 0;
                Rect b = topbar.getBounds();
                topbar.setBounds(new Rect(0, 0, 16, 16));
                topbar.setBounds(b);
            }
        };
        hsc.loadPage(integer);
        windowMaker.accept(topbar);
    }
}
