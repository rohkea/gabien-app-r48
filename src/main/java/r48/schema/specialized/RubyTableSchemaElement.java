/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.schema.specialized;

import gabien.FontManager;
import gabien.IGrDriver;
import gabien.ui.*;
import r48.FontSizes;
import r48.RubyIO;
import r48.RubyTable;
import r48.dbs.PathSyntax;
import r48.dbs.TXDB;
import r48.schema.AggregateSchemaElement;
import r48.schema.SchemaElement;
import r48.schema.specialized.tbleditors.ITableCellEditor;
import r48.schema.util.ISchemaHost;
import r48.schema.util.SchemaPath;
import r48.ui.UIGrid;

/**
 * OLD:
 * Not really finished
 * Created on 12/29/16.
 * NEW:
 * Kind of finished
 * Worked on at 01/03/16, comment rewritten very, very early technically the next day,
 * after AutoTileRules was added
 * NEWER: Abstractified at midnight. That is, 0:00 February 18th 2017, where I count 8 hours after that as being the same day.
 * ---LATER STILL---
 * Checking for the monitorsSubelements requirement...
 * no, the resize does the correct corrections, I believe.
 */
public class RubyTableSchemaElement<TileHelper> extends SchemaElement {
    public final int defW;
    public final int defH;
    public final int planes, dimensions;
    public final String iVar, widthVar, heightVar;
    public final ITableCellEditor tableCellEditor;

    public final int[] defVals;

    public boolean allowTextdraw = true;
    public boolean allowResize = true;

    // NOTE: Doesn't need SDB2-PS compat because it only just started using PS, thankfully
    public RubyTableSchemaElement(String iVar, String wVar, String hVar, int dim, int dw, int dh, int defL, ITableCellEditor tcl, int[] defV) {
        this.iVar = iVar;
        widthVar = wVar;
        heightVar = hVar;
        defW = dw;
        defH = dh;
        planes = defL;
        dimensions = dim;
        tableCellEditor = tcl;
        defVals = defV;
    }

    @Override
    public UIElement buildHoldingEditor(final RubyIO target, final ISchemaHost launcher, final SchemaPath path) {
        final RubyIO targV = iVar == null ? target : PathSyntax.parse(target, iVar, true);
        final RubyTable targ = new RubyTable(targV.userVal);
        final RubyIO width = widthVar == null ? null : PathSyntax.parse(target, widthVar, true);
        final RubyIO height = heightVar == null ? null : PathSyntax.parse(target, heightVar, true);

        final SchemaPath dataBlackboxTarget = path.findLast();
        final SchemaPath.EmbedDataKey blackboxKey = new SchemaPath.EmbedDataKey(this, targV, RubyTableSchemaElement.class, "blackbox");

        int gridSize = getGridSize();
        final UIGrid uig = new UIGrid(gridSize, gridSize, targ.width * targ.height) {
            private TileHelper tileHelper;

            @Override
            protected void drawTile(int t, boolean hover, int x, int y, IGrDriver igd) {
                int tX = t % targ.width;
                int tY = t / targ.width;
                if (targ.outOfBounds(tX, tY))
                    return;
                tileHelper = baseTileDraw(target, t, x, y, igd, tileHelper);
                if (allowTextdraw) {
                    igd.clearRect(0, 0, 0, x, y, tileSizeW, FontSizes.gridTextHeight);
                    for (int i = 0; i < targ.planeCount; i++)
                        FontManager.drawString(igd, x, y + (i * FontSizes.gridTextHeight), Integer.toHexString(targ.getTiletype(t % targ.width, t / targ.width, i) & 0xFFFF), false, false, FontSizes.gridTextHeight);
                }
            }

            @Override
            public void update(double deltaTime) {
                super.update(deltaTime);
                // Lack of any better place.
                double v = uivScrollbar.scrollPoint;
                if (v <= 0)
                    v = 0;
                if (v >= 0.99)
                    v = 0.99;
                v += getSelected();
                dataBlackboxTarget.getEmbedMap(launcher).put(blackboxKey, v);
            }
        };

        final UIScrollLayout uiSVL = AggregateSchemaElement.createScrollSavingSVL(path, launcher, this, target);
        final Runnable editorOnSelChange = tableCellEditor.createEditor(uiSVL, targV, uig, new Runnable() {
            @Override
            public void run() {
                path.changeOccurred(false);
            }
        });
        uig.onSelectionChange = new Runnable() {
            int selectionOnLastCall = -1;

            @Override
            public void run() {
                int sel = uig.getSelected();
                int oldSel = selectionOnLastCall;
                selectionOnLastCall = sel;
                editorOnSelChange.run();
                if (oldSel == sel) {
                    int tX = sel % targ.width;
                    int tY = sel / targ.width;
                    short p = targ.getTiletype(tX, tY, 0);
                    short p2 = baseFlipBits(p);
                    if (p != p2) {
                        targ.setTiletype(tX, tY, 0, p2);
                        path.changeOccurred(false);
                    }
                }
            }
        };

        // and now time for your daily dose of magic:
        // the scroll value here holds both the selection & the scroll cache.
        Double selVal = dataBlackboxTarget.getEmbedMap(launcher).get(blackboxKey);
        if (selVal == null)
            selVal = -1d;
        // -1d + 0.99 = -0.01d
        // Clarify this situation first. Note that Math.floor extends to negative infinity, which is perfect for this
        int lastSelectionCache = (int) Math.floor(selVal);
        double lastScrollCache = selVal - lastSelectionCache;
        if (lastScrollCache <= 0)
            lastScrollCache = 0;
        if (lastScrollCache >= 0.985)
            lastScrollCache = 1;
        uig.setSelected(lastSelectionCache);
        uig.uivScrollbar.scrollPoint = lastScrollCache;

        if (allowResize) {
            final UINumberBox wNB = new UINumberBox(targ.width, FontSizes.tableSizeTextHeight);
            wNB.onEdit = new Runnable() {
                @Override
                public void run() {
                    if (wNB.number < 0)
                        wNB.number = 0;
                }
            };
            final UINumberBox hNB = new UINumberBox(targ.height, FontSizes.tableSizeTextHeight);
            hNB.onEdit = new Runnable() {
                @Override
                public void run() {
                    if (hNB.number < 0)
                        hNB.number = 0;
                }
            };
            UIElement uie = new UISplitterLayout(wNB, hNB, false, 1, 2);
            uiSVL.panelsAdd(uie);
            uiSVL.panelsAdd(new UITextButton(TXDB.get("Resize"), FontSizes.tableResizeTextHeight, new Runnable() {
                @Override
                public void run() {
                    int w = (int) wNB.number;
                    if (w < 0)
                        w = 0;
                    int h = (int) hNB.number;
                    if (h < 0)
                        h = 0;
                    RubyTable r2 = targ.resize(w, h, defVals);
                    if (width != null)
                        width.fixnumVal = w;
                    if (height != null)
                        height.fixnumVal = h;
                    targV.userVal = r2.innerBytes;
                    path.changeOccurred(false);
                }
            }));
        }
        return new UISplitterLayout(uig, uiSVL, false, 6, 8);
    }

    // Overridden in super-special tileset versions of this.
    // The idea is that TileHelper can contain any helper object needed.
    public TileHelper baseTileDraw(RubyIO target, int t, int x, int y, IGrDriver igd, TileHelper th) {
        return null;
    }

    public short baseFlipBits(short p) {
        return p;
    }

    @Override
    public void modifyVal(RubyIO target, SchemaPath index, boolean setDefault) {
        if (iVar != null) {
            RubyIO st = target.getInstVarBySymbol(iVar);
            if (st == null) {
                st = new RubyIO(); // will trigger change because type != 'u'
                target.addIVar(iVar, st);
            }
            target = st;
        }
        // Not a clue, so re-initialize if all else fails.
        // (This will definitely trigger if the iVar was missing)
        boolean changeOccurred = false;
        if (target.type != 'u') {
            target.setUser("Table", new RubyTable(dimensions, defW, defH, planes, defVals).innerBytes);
            changeOccurred = true;
        }
        // Fix up pre v1.0-2 tables (would have existed from the start if I knew about it, but...)
        RubyTable rt = new RubyTable(target.userVal);
        if (rt.dimensionCount != dimensions) {
            rt.innerTable.putInt(0, dimensions);
            changeOccurred = true;
        }
        if (changeOccurred)
            index.changeOccurred(true);
    }

    public int getGridSize() {
        return FontSizes.scaleGrid(32);
    }
}
