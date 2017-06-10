/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabienapp;

import gabien.GaBIEn;
import gabien.ui.*;
import r48.AppMain;
import r48.FontSizes;
import r48.RubyIO;
import r48.dbs.DBLoader;
import r48.dbs.IDatabase;
import r48.map.UIMapToolWrapper;
import r48.map.UIMapView;
import r48.ui.UIFontSizeConfigurator;
import r48.ui.UIHHalfsplit;
import gabien.ui.UIScrollLayout;

import java.io.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created on 1/27/17.
 */
public class Application {
    public static int globalMS = 33;
    private static IConsumer<Double> appTicker = null;

    public static void gabienmain() throws IOException {
        final WindowCreatingUIElementConsumer uiTicker = new WindowCreatingUIElementConsumer();

        final UIScrollLayout gamepaks = new UIScrollLayout(true);
        gamepaks.setBounds(new Rect(0, 0, 400, 200));
        // this can't be good
        // Ok, explaination for this. Giving it a runnable, it will hold it until calld again, and then it will run it and remove it.
        final IConsumer<Runnable> closeHelper = new IConsumer<Runnable>() {
            private Runnable r;

            @Override
            public void accept(Runnable runnable) {
                if (runnable != null) {
                    r = runnable;
                } else {
                    r.run();
                    r = null;
                }
            }
        };

        UIAdjuster msAdjust = new UIAdjuster(FontSizes.launcherTextHeight, new ISupplier<String>() {
            @Override
            public String get() {
                return Integer.toString(++globalMS);
            }
        }, new ISupplier<String>() {
            @Override
            public String get() {
                if (globalMS == 1)
                    return Integer.toString(globalMS);
                return Integer.toString(--globalMS);
            }
        });
        msAdjust.accept(Integer.toString(globalMS));

        gamepaks.panels.add(new UIHHalfsplit(1, 2, new UITextButton(FontSizes.launcherTextHeight, "Quit", new Runnable() {
            @Override
            public void run() {
                GaBIEn.ensureQuit();
            }
        }), new UITextButton(FontSizes.launcherTextHeight, "Font Cfg.", new Runnable() {
            @Override
            public void run() {
                uiTicker.accept(new UIFontSizeConfigurator());
            }
        })));

        gamepaks.panels.add(new UIHHalfsplit(3, 5, new UILabel("msPerFrame:", FontSizes.launcherTextHeight), msAdjust));

        gamepaks.panels.add(new UILabel("Root Path:", FontSizes.launcherTextHeight));

        final UITextBox rootBox = new UITextBox(FontSizes.launcherTextHeight);
        gamepaks.panels.add(rootBox);

        gamepaks.panels.add(new UILabel("Choose Target Engine:", FontSizes.launcherTextHeight));

        DBLoader.readFile("Gamepaks.txt", new IDatabase() {

            UITextButton lastButton;
            AtomicReference<String> boxedEncoding; // it's a boxed object, so...

            @Override
            public void newObj(int objId, final String objName) throws IOException {
                String status = "?";
                if (objId == 0)
                    status = "Working";
                if (objId == 1)
                    status = "Missing Parts";
                if (objId == 2)
                    status = "Minimal/Useless";
                if (objId == 3)
                    status = "Non-existent";
                final AtomicReference<String> box = new AtomicReference<String>();
                boxedEncoding = box;
                lastButton = new UITextButton(FontSizes.enumChoiceTextHeight, objName + " (" + status + ")", new Runnable() {
                    @Override
                    public void run() {
                        if (appTicker == null) {
                            try {
                                RubyIO.encoding = box.get();
                                String rootPath = rootBox.text;
                                if (!rootPath.equals(""))
                                    if (!rootPath.endsWith("/"))
                                        if (!rootPath.endsWith("\\"))
                                            rootPath += "/";
                                appTicker = AppMain.initializeAndRun(rootPath, objName + "/", uiTicker);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        closeHelper.accept(null);
                    }
                });
                InputStream tester = GaBIEn.getFile(objName + "/Schema.txt");
                if (tester != null) {
                    gamepaks.panels.add(lastButton);
                    tester.close();
                }
            }

            @Override
            public void execCmd(char c, String[] args) throws IOException {
                if (c == '.') {
                    String rn = "";
                    for (String s : args)
                        rn += s + " ";
                    lastButton.Text = rn;
                }
                if (c == 'e')
                    boxedEncoding.set(args[0]);

                /*
                 * if (c == 'f')
                 *     if (!new File(args[0]).exists()) {
                 *         System.out.println("Can't use " + lastButton.Text + ": " + args[0] + " missing");
                 *         gamepaks.panels.remove(lastButton);
                 *     }
                 */
            }
        });

        while (true) {
            gamepaks.setBounds(new Rect(0, 0, 640, 480));
            final UIMapToolWrapper uimtw = new UIMapToolWrapper(gamepaks);
            closeHelper.accept(new Runnable() {
                @Override
                public void run() {
                    uimtw.selfClose = true;
                }
            });
            uiTicker.accept(uimtw);

            while (uiTicker.runningWindows() > 0) {
                double dT = GaBIEn.timeDelta(false);
                while (dT < (globalMS / 1000d)) {
                    try {
                        Thread.sleep(globalMS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    dT = GaBIEn.timeDelta(false);
                }
                dT = GaBIEn.timeDelta(true);
                if (appTicker != null)
                    appTicker.accept(dT);
                uiTicker.runTick(dT);
            }
            if (!uimtw.selfClose)
                break;
            appTicker = null;
        }
        GaBIEn.ensureQuit();
    }
}
