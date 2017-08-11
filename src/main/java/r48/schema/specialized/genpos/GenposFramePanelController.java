/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package r48.schema.specialized.genpos;

import gabien.IGrInDriver;
import gabien.IImage;
import gabien.ui.Rect;
import gabien.ui.UISplitterLayout;
import gabien.ui.UITextButton;
import r48.FontSizes;
import r48.dbs.TXDB;
import r48.schema.util.ISchemaHost;
import r48.ui.UINSVertLayout;

/**
 * Part of genpos.
 * This takes some of RMAnim's responsibilities.
 * Created on 28/07/17.
 */
public class GenposFramePanelController {
    public UICellSelectionPanel cellSelection;
    public UICellEditingPanel editingPanel;
    public UISingleFrameView editor;
    public UINSVertLayout editingSidebar;

    // For use by the parent.
    public UISplitterLayout rootLayout;
    public IGenposFrame frame;

    // for schema purposes
    public ISchemaHost hostLauncher;

    public UITextButton gridToggleButton;

    public GenposFramePanelController(IGenposFrame rootForNow, ISchemaHost launcher) {
        hostLauncher = launcher;
        frame = rootForNow;
        editor = new UISingleFrameView(this);
        IImage bkg = rootForNow.getBackground();
        if (bkg != null) {
            editor.camX = bkg.getWidth() / 2;
            editor.camY = bkg.getHeight() / 2;
        }
        cellSelection = new UICellSelectionPanel(rootForNow);

        editingPanel = new UICellEditingPanel(cellSelection, this);
        gridToggleButton = new UITextButton(FontSizes.rmaCellFontSize, TXDB.get("8px Grid"), new Runnable() {
            @Override
            public void run() {
                // Do nothing.
            }
        }).togglable();
        editingSidebar = new UINSVertLayout(gridToggleButton, new UINSVertLayout(editingPanel, cellSelection));
        // Set an absolute width for the editing sidebar
        editingSidebar.setBounds(new Rect(0, 0, 192, 32));
        rootLayout = new UISplitterLayout(editor, editingSidebar, false, 1);
    }

    // Frame changed events <Incoming>. Run before displaying on-screen
    public void frameChanged() {
        // This implicitly changes an incrementing number which causes the cell editor to update, but, that happens at next frame.
        // Instead, for a case like this, call editingPanel directly
        cellSelection.frameChanged();
        editingPanel.somethingChanged();
    }


}
