/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package r48.map;

import gabien.IGrInDriver;
import gabien.ui.*;
import r48.AppMain;
import r48.FontSizes;
import r48.dbs.TXDB;
import r48.map.systems.MapSystem;
import r48.map.tiles.VXATileRenderer;
import r48.maptools.*;
import r48.ui.UINSVertLayout;

import java.util.LinkedList;

/**
 * WARNING: May Contain Minigame.
 * Created on 1/1/17.
 */
public class UIMapViewContainer extends UIPanel {
    private final ISupplier<IConsumer<UIElement>> windowMakerSupplier;
    public UIMapView view;
    private UINSVertLayout viewToolbarSplit;
    // Use when mapTool is being set to null.
    private Runnable internalNoToolCallback = new Runnable() {
        @Override
        public void run() {

        }
    };

    // Map tool switch happens at the start of each frame, so it stays out of the way of windowing code.
    private UIMapToolWrapper mapTool = null;
    private boolean wantsToolHide = false;

    private TimeWaster timeWaster = new TimeWaster();

    public UIMapViewContainer(ISupplier<IConsumer<UIElement>> wms) {
        windowMakerSupplier = wms;
    }

    @Override
    public void setBounds(Rect r) {
        super.setBounds(r);
        //iconPlanX = (r.width / 2) - 32;
        //iconPlanY = (r.textHeight / 2) - 32;
        if (view != null)
            viewToolbarSplit.setBounds(new Rect(0, 0, r.width, r.height));
    }

    @Override
    public void updateAndRender(int ox, int oy, double deltaTime, boolean select, IGrInDriver igd) {

        // remove stale tools.
        // (The way this code is written implies tools must be on rootView for now.)
        if (mapTool != null) {
            if (wantsToolHide)
                mapTool.selfClose = true;
            if (mapTool.hasClosed) {
                if (AppMain.nextMapTool == mapTool.pattern)
                    AppMain.nextMapTool = null;
                mapTool = null;
                internalNoToolCallback.run();
                if (view != null)
                    view.callbacks = null;
            }
        }
        wantsToolHide = false;
        // switch to next tool
        if (view != null) {
            if (AppMain.nextMapTool != null) {
                boolean sameAsBefore = true;
                if (mapTool != null) {
                    if (mapTool.pattern != AppMain.nextMapTool) {
                        // let's just hope the user doesn't do anything in a frame
                        // that would actually somehow lead to an inconsistent state
                        mapTool.selfClose = true;
                        sameAsBefore = false;
                    }
                } else {
                    sameAsBefore = false;
                }
                if (!sameAsBefore) {
                    view.callbacks = null;
                    if (AppMain.nextMapTool instanceof IMapViewCallbacks)
                        view.callbacks = (IMapViewCallbacks) AppMain.nextMapTool;
                    mapTool = new UIMapToolWrapper(AppMain.nextMapTool);
                    windowMakerSupplier.get().accept(mapTool);
                }
            } else {
                if (mapTool != null) {
                    mapTool.selfClose = true;
                    mapTool = null;
                    internalNoToolCallback.run();
                    view.callbacks = null;
                }
            }
        }

        super.updateAndRender(ox, oy, deltaTime, select, igd);
        if (view != null)
            return;
        Rect r = getBounds();
        timeWaster.draw(igd, ox, oy, deltaTime, r.width, r.height);
    }

    public void loadMap(MapSystem.MapLoadDetails map) {
        wantsToolHide = true;
        allElements.clear();
        if (view != null)
            view.windowClosed();
        if (map == null) {
            view = null;
            internalNoToolCallback = new Runnable() {
                @Override
                public void run() {
                }
            };
            return;
        }
        Rect b = getBounds();
        // Creating the MapView and such causes quite a few side-effects (specifically global StuffRenderer kick-in-the-pants).
        // Also kick the dictionaries because of the event dictionary.
        view = new UIMapView(map.objectId, b.width, b.height);
        view.pickTileHelper = new IConsumer<Short>() {
            @Override
            public void accept(Short aShort) {
                UIMTAutotile atf = new UIMTAutotile(view);
                atf.selectTile(aShort);
                AppMain.nextMapTool = atf;
            }
        };

        final IEditingToolbarController metc = map.getToolbar.apply(view);
        viewToolbarSplit = new UINSVertLayout(metc.getBar(), view);
        allElements.add(viewToolbarSplit);
        AppMain.schemas.kickAllDictionariesForMapChange();
        internalNoToolCallback = new Runnable() {
            @Override
            public void run() {
                metc.noTool();
            }
        };
        metc.noTool();
    }
}
