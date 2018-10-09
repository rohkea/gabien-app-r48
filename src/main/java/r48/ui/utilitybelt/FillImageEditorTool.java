/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.ui.utilitybelt;

import gabien.ui.*;
import r48.FontSizes;
import r48.dbs.TXDB;

/**
 * Created on October 09, 2018.
 */
public class FillImageEditorTool implements IImageEditorTool {
    public boolean autoshade, autoshadeLRX, autoshadeUDX;

    @Override
    public void enter(UIImageEditView uiev) {

    }

    @Override
    public void apply(UIImageEditView.ImPoint imp, final UIImageEditView view, boolean major, boolean dragging) {
        if ((!major) || dragging)
            return;
        view.eds.startSection();
        final int spi = view.image.getRaw(imp.correctedX, imp.correctedY);
        FillAlgorithm fa = new FillAlgorithm(new IFunction<FillAlgorithm.Point, FillAlgorithm.Point>() {
            @Override
            public FillAlgorithm.Point apply(FillAlgorithm.Point point) {
                UIImageEditView.ImPoint imp = new UIImageEditView.ImPoint(point.x, point.y);
                imp.updateCorrected(view);
                if (view.tiling == null) {
                    if (imp.correctedX != point.x)
                        return null;
                    if (imp.correctedY != point.y)
                        return null;
                }
                return new FillAlgorithm.Point(imp.correctedX, imp.correctedY);
            }
        }, new IFunction<FillAlgorithm.Point, Boolean>() {
            @Override
            public Boolean apply(FillAlgorithm.Point point) {
                return view.image.getRaw(point.x, point.y) == spi;
            }
        });
        fa.availablePointSet.add(new FillAlgorithm.Point(imp.correctedX, imp.correctedY));
        while (!fa.availablePointSet.isEmpty())
            fa.pass();
        int shA = Math.max(view.selPaletteIndex - 1, 0);
        int shB = view.selPaletteIndex;
        int shC = Math.min(view.selPaletteIndex + 1, view.image.paletteSize() - 1);
        for (FillAlgorithm.Point p : fa.executedPointSet) {
            boolean above = !fa.executedPointSet.contains(tileAS(view, p.offset(0, -1)));
            boolean below = !fa.executedPointSet.contains(tileAS(view, p.offset(0, 1)));
            boolean left = !fa.executedPointSet.contains(tileAS(view, p.offset(-1, 0)));
            boolean right = !fa.executedPointSet.contains(tileAS(view, p.offset(1, 0)));

            if (above && (!below)) {
                if (autoshade) {
                    view.image.setRaw(p.x, p.y, autoshadeUDX ? shC : shA);
                } else {
                    view.image.setRaw(p.x, p.y, shB);
                }
            } else if ((!above) && below) {
                if (autoshade) {
                    view.image.setRaw(p.x, p.y, autoshadeUDX ? shA : shC);
                } else {
                    view.image.setRaw(p.x, p.y, shB);
                }
            } else if (left && (!right)) {
                if (autoshade) {
                    view.image.setRaw(p.x, p.y, autoshadeLRX ? shC : shA);
                } else {
                    view.image.setRaw(p.x, p.y, shB);
                }
            } else if ((!left) && right) {
                if (autoshade) {
                    view.image.setRaw(p.x, p.y, autoshadeLRX ? shA : shC);
                } else {
                    view.image.setRaw(p.x, p.y, shB);
                }
            } else {
                view.image.setRaw(p.x, p.y, shB);
            }
        }
        view.eds.endSection();
    }

    // Doesn't have to return inbound results, does have to return non-null ones
    private FillAlgorithm.Point tileAS(UIImageEditView view, FillAlgorithm.Point point) {
        if (view.tiling == null)
            return point;
        UIImageEditView.ImPoint imp = new UIImageEditView.ImPoint(point.x, point.y);
        imp.updateCorrected(view);
        return new FillAlgorithm.Point(imp.correctedX, imp.correctedY);
    }

    @Override
    public void endApply(UIImageEditView view) {

    }

    @Override
    public UIElement createToolPalette(UIImageEditView uiev) {
        UIElement uie = RootImageEditorTool.createToolPalette(uiev, FillImageEditorTool.class);
        UIScrollLayout usl = new UIScrollLayout(false, FontSizes.mapToolbarScrollersize);
        usl.panelsAdd(new UITextButton(TXDB.get("Autoshade"), FontSizes.imageEditorTextHeight, new Runnable() {
            @Override
            public void run() {
                autoshade = !autoshade;
            }
        }).togglable(autoshade));
        usl.panelsAdd(new UITextButton(TXDB.get("LR"), FontSizes.imageEditorTextHeight, new Runnable() {
            @Override
            public void run() {
                autoshadeLRX = !autoshadeLRX;
            }
        }).togglable(autoshadeLRX));
        usl.panelsAdd(new UITextButton(TXDB.get("UD"), FontSizes.imageEditorTextHeight, new Runnable() {
            @Override
            public void run() {
                autoshadeUDX = !autoshadeUDX;
            }
        }).togglable(autoshadeUDX));
        return new UISplitterLayout(uie, usl, true, 1.0);
    }

    @Override
    public Rect getSelection() {
        return null;
    }

    @Override
    public String getLocalizedText(boolean dedicatedDragControl) {
        return TXDB.get("Press to fill area.");
    }

    @Override
    public IImageEditorTool getCamModeLT() {
        return null;
    }
}
