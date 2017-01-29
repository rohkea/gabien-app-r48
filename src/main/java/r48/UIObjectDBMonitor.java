/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package r48;

import gabien.IGrInDriver;
import gabien.ui.UIElement;
import gabien.ui.UILabel;

/**
 * Used to make sure nothing's leaking memory.
 * Very performance-eating because it does a GC every frame to keep itself accurate.
 * Created on 12/29/16.
 */
public class UIObjectDBMonitor extends UIElement {
    @Override
    public void updateAndRender(int ox, int oy, double deltaTime, boolean selected, IGrInDriver igd) {
        System.gc();
        for (String s : UITest.sortedKeysStr(AppMain.objectDB.objectMap.keySet())) {
            String status = " [disposed]";
            RubyIO rio = AppMain.objectDB.objectMap.get(s).get();
            if (rio != null) {
                status = " [" + AppMain.objectDB.countModificationListeners(rio) + "ML]";
                if (AppMain.objectDB.getObjectModified(s))
                    status += " [Modified]";
            } else {
                if (AppMain.objectDB.getObjectModified(s)) {
                    status += " [Modifications lost]";
                } else {
                    AppMain.objectDB.objectMap.remove(s);
                }
            }
            UILabel.drawLabel(igd, getBounds().width, ox, oy, s + status, false, FontSizes.objectDBMonitorTextHeight);
            oy += 9;
        }
    }

    @Override
    public void handleClick(int x, int y, int button) {

    }
}
