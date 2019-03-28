/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.tests.grand;

import gabien.MobilePeripherals;
import gabien.TestKickstart;
import gabien.ui.ISupplier;
import gabien.ui.UIElement;
import gabienapp.Application;
import r48.io.IntUtils;
import r48.wm.GrandWindowManagerUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Created on March 28, 2019.
 */
public class GrandTestBuilder {
    public void thenWaitFrame() {
        TestKickstart.waitingTestEntries.add(new ISupplier<Boolean>() {
            boolean waitFrame = true;

            @Override
            public Boolean get() {
                if (waitFrame) {
                    waitFrame = false;
                    return false;
                }
                return true;
            }
        });
    }

    public void thenClick(final int i, final int i1) {
        TestKickstart.waitingTestEntries.add(new ISupplier<Boolean>() {
            boolean waitFrame = true;

            @Override
            public Boolean get() {
                if (waitFrame) {
                    TestKickstart.pointer = new MobilePeripherals.DummyPointer(i, i1);
                    waitFrame = false;
                    return false;
                }
                TestKickstart.pointer = null;
                return true;
            }
        });
    }

    public void thenDrag(final int i, final int i1, final int i2, final int i3) {
        TestKickstart.waitingTestEntries.add(new ISupplier<Boolean>() {
            int waitFrame = 0;

            @Override
            public Boolean get() {
                if (waitFrame == 0) {
                    TestKickstart.pointer = new MobilePeripherals.DummyPointer(i, i1);
                    waitFrame++;
                    return false;
                } else if (waitFrame == 1) {
                    TestKickstart.pointer.x = i2;
                    TestKickstart.pointer.y = i3;
                    waitFrame++;
                    return false;
                }
                TestKickstart.pointer = null;
                return true;
            }
        });
    }

    public void thenWaitWC(final int wc) {
        TestKickstart.waitingTestEntries.add(new ISupplier<Boolean>() {
            @Override
            public Boolean get() {
                return TestKickstart.windowCount == wc;
            }
        });
    }

    private UIElement getElement(final int widx, final int exwlen) {
        UIElement[] w = GrandWindowManagerUtils.getAllWindows();
        if (w.length != exwlen)
            throw new GrandExecutionError("Expected wlen != actual wlen");
        if (widx >= w.length)
            throw new GrandExecutionError("Invalid index...");
        return w[widx];
    }

    private UIElement getElement(String s) {
        UIElement[] w = GrandWindowManagerUtils.getAllWindows();
        for (UIElement uie : w)
            if (uie.toString().contains(s))
                return uie;
        throw new GrandExecutionError("Unable to getElement " + s);
    }

    public void thenIcon(final int widx, final int exwlen, final int idx) {
        TestKickstart.waitingTestEntries.add(new ISupplier<Boolean>() {
            boolean waitFrame = true;

            @Override
            public Boolean get() {
                if (waitFrame) {
                    GrandWindowManagerUtils.clickIcon(getElement(widx, exwlen), idx);
                    waitFrame = false;
                    return false;
                }
                return true;
            }
        });
    }

    public void thenIcon(final String title, final int idx) {
        TestKickstart.waitingTestEntries.add(new ISupplier<Boolean>() {
            boolean waitFrame = true;

            @Override
            public Boolean get() {
                if (waitFrame) {
                    GrandWindowManagerUtils.clickIcon(getElement(title), idx);
                    waitFrame = false;
                    return false;
                }
                return true;
            }
        });
    }

    public void thenSelectTab(final int widx, final int exwlen) {
        TestKickstart.waitingTestEntries.add(new ISupplier<Boolean>() {
            boolean waitFrame = true;

            @Override
            public Boolean get() {
                if (waitFrame) {
                    GrandWindowManagerUtils.selectTab(getElement(widx, exwlen));
                    waitFrame = false;
                    return false;
                }
                return true;
            }
        });
    }

    public void thenSelectTab(final String title) {
        TestKickstart.waitingTestEntries.add(new ISupplier<Boolean>() {
            boolean waitFrame = true;

            @Override
            public Boolean get() {
                if (waitFrame) {
                    GrandWindowManagerUtils.selectTab(getElement(title));
                    waitFrame = false;
                    return false;
                }
                return true;
            }
        });
    }


    public void thenType(final String s) {
        TestKickstart.waitingTestEntries.add(new ISupplier<Boolean>() {
            boolean waitFrame = true;

            @Override
            public Boolean get() {
                if (waitFrame) {
                    TestKickstart.maintainText = s;
                    TestKickstart.maintainTextEnter = true;
                    waitFrame = false;
                    return false;
                }
                return true;
            }
        });
    }

    public void thenCloseWindow() {
        TestKickstart.waitingTestEntries.add(new ISupplier<Boolean>() {
            boolean waitFrame = true;

            @Override
            public Boolean get() {
                if (waitFrame) {
                    TestKickstart.windows.getLast().shutdown();
                    waitFrame = false;
                    return false;
                }
                return true;
            }
        });
    }

    public void execute(long expectedChecksum) throws IOException {
        TestKickstart.kickstartRFS();
        Application.gabienmain();
        FileOutputStream fos = new FileOutputStream("test-debug.pak");
        byte[] dat = createDump();
        fos.write(dat);
        fos.close();
        long checksum = 0;
        for (byte b : dat)
            checksum += b & 0xFF;
        if (expectedChecksum != checksum)
            throw new RuntimeException("Checksum mismatch. Expected " + checksum);
    }

    private byte[] createDump() throws IOException {
        LinkedList<DumpedLump> l = new LinkedList<DumpedLump>();

        LinkedList<String> lls = new LinkedList<String>(TestKickstart.mockFS.keySet());
        Collections.sort(lls);

        for (String s : lls)
            l.add(new DumpedLump(s, TestKickstart.mockFS.get(s)));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        int knownPos = 12 + (l.size() * 64);
        baos.write('P');
        baos.write('A');
        baos.write('C');
        baos.write('K');
        IntUtils.writeS32(baos, 12);
        IntUtils.writeS32(baos, l.size() * 64);
        for (DumpedLump dl : l) {
            byte[] baseName = new byte[56];
            int idx = 0;
            for (byte b : dl.name.getBytes("UTF-8"))
                baseName[idx++] = b;
            baos.write(baseName);
            IntUtils.writeS32(baos, knownPos);
            IntUtils.writeS32(baos, dl.data.length);
            knownPos += dl.data.length;
        }
        for (DumpedLump dl : l) {
            baos.write(dl.data);
        }
        return baos.toByteArray();
    }

    private static class DumpedLump {
        String name;
        byte[] data;

        public DumpedLump(String file, byte[] bytes) {
            name = file;
            data = bytes;
        }
    }
}
