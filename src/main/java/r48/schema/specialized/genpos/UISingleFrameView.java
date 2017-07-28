/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package r48.schema.specialized.genpos;

import gabien.IGrInDriver;
import gabien.ScissorGrInDriver;
import gabien.ui.Rect;
import gabien.ui.UIElement;
import r48.RubyIO;
import r48.RubyTable;

/**
 * Handles drawing for a single-frame editor.
 * Created on 2/17/17.
 */
public class UISingleFrameView extends UIElement {
    public GenposFramePanelController basePanelAccess;

    private int lastMX, lastMY, camX, camY;
    private int dragging;

    public UISingleFrameView(GenposFramePanelController rmAnimRootPanel) {
        basePanelAccess = rmAnimRootPanel;
    }

    @Override
    public void updateAndRender(int ox, int oy, double deltaTime, boolean select, IGrInDriver igdo) {
        ScissorGrInDriver igd = new ScissorGrInDriver();

        Rect b = getBounds();

        igd.inner = igdo;
        igd.workLeft = ox;
        igd.workTop = oy;
        igd.workRight = ox + b.width;
        igd.workBottom = oy + b.height;

        igd.clearAll(255, 0, 255);

        int opx = ox + (b.width / 2) - camX;
        int opy = oy + (b.height / 2) - camY;

        IGrInDriver.IImage bkg = basePanelAccess.frame.getBackground();
        if (bkg != null) {
            // Draw the background in the centre
            igd.blitImage(0, 0, bkg.getWidth(), bkg.getHeight(), -bkg.getWidth() / 2, -bkg.getHeight() / 2, bkg);
        }
        int[] d = basePanelAccess.frame.getIndicators();
        for (int i = 0; i < d.length; i += 2) {
            int x = d[i];
            int y = d[i + 1];
            igd.clearRect(192, 0, 192, (opx + x) - 8, (opy + y) - 1, 16, 2);
            igd.clearRect(192, 0, 192, (opx + x) - 1, (opy + y) - 8, 2, 16);
        }
        int cellCount = basePanelAccess.frame.getCellCount();
        for (int i = 0; i < cellCount; i++)
            basePanelAccess.frame.drawCell(i, opx, opy, igd);
    }

    @Override
    public void handleClick(int x, int y, int button) {
        dragging = button;
        lastMX = x;
        lastMY = y;
    }

    @Override
    public void handleDrag(int x, int y) {
        if (dragging == 1) {
            if (basePanelAccess.cellSelection.cellNumber != -1) {
                // RubyIO target = basePanelAccess.frame.getFrame();
                // RubyTable rt = new RubyTable(target.getInstVarBySymbol("@cell_data").userVal);
                int ofsX = x - lastMX;
                int ofsY = y - lastMY;
                basePanelAccess.frame.setCellProp(basePanelAccess.cellSelection.cellNumber, 1, (short) (basePanelAccess.frame.getCellProp(basePanelAccess.cellSelection.cellNumber, 1) + ofsX));
                basePanelAccess.frame.setCellProp(basePanelAccess.cellSelection.cellNumber, 2, (short) (basePanelAccess.frame.getCellProp(basePanelAccess.cellSelection.cellNumber, 2) + ofsY));
            }
        } else if (dragging == 3) {
            camX -= x - lastMX;
            camY -= y - lastMY;
        }
        lastMX = x;
        lastMY = y;
    }
}
