/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package r48.map.systems;

import gabien.IGrInDriver;
import gabien.ui.IConsumer;
import gabien.ui.ISupplier;
import gabien.ui.UIElement;
import r48.AppMain;
import r48.RubyIO;
import r48.RubyTable;
import r48.map.StuffRenderer;
import r48.map.UIMapViewContainer;
import r48.map.drawlayers.EventMapViewDrawLayer;
import r48.map.drawlayers.IMapViewDrawLayer;
import r48.map.drawlayers.PanoramaMapViewDrawLayer;
import r48.map.drawlayers.R2kTileMapViewDrawLayer;
import r48.map.events.IEventGraphicRenderer;
import r48.map.events.R2kEventGraphicRenderer;
import r48.map.imaging.CacheImageLoader;
import r48.map.imaging.IImageLoader;
import r48.map.imaging.XYZOrPNGImageLoader;
import r48.map.mapinfos.R2kRMLikeMapInfoBackend;
import r48.map.mapinfos.UIGRMMapInfos;
import r48.map.tiles.ITileRenderer;
import r48.map.tiles.LcfTileRenderer;

/**
 * ...
 * Created on 03/06/17.
 */
public class R2kSystem extends MapSystem {
    @Override
    public UIElement createMapExplorer(ISupplier<IConsumer<UIElement>> windowMaker, UIMapViewContainer mapBox) {
        return new UIGRMMapInfos(windowMaker, mapBox, new R2kRMLikeMapInfoBackend());
    }

    private RubyIO tsoFromMap2000(RubyIO map) {
        if (map == null)
            return null;
        return AppMain.objectDB.getObject("RPG_RT.ldb").getInstVarBySymbol("@tilesets").getHashVal(map.getInstVarBySymbol("@tileset_id"));
    }

    @Override
    public StuffRenderer rendererFromMap(RubyIO map) {
        IImageLoader imageLoader = new CacheImageLoader(new XYZOrPNGImageLoader(AppMain.rootPath));
        RubyIO tileset = tsoFromMap2000(map);
        ITileRenderer tileRenderer = new LcfTileRenderer(imageLoader, tileset);
        IEventGraphicRenderer eventRenderer = new R2kEventGraphicRenderer(imageLoader, tileRenderer);
        IMapViewDrawLayer[] layers = new IMapViewDrawLayer[0];
        if (map != null) {
            RubyIO events = map.getInstVarBySymbol("@events");
            RubyTable tbl = new RubyTable(map.getInstVarBySymbol("@data").userVal);
            String vxaPano = map.getInstVarBySymbol("@parallax_name").decString();
            if (map.getInstVarBySymbol("@parallax_flag").type != 'T')
                vxaPano = "";
            layers = new IMapViewDrawLayer[8];
            IGrInDriver.IImage img = null;
            if (!vxaPano.equals(""))
                img = imageLoader.getImage("Panorama/" + vxaPano, true);
            // Layer order seems to be this:
            // layer 1 lower
            // layer 2 lower
            // <events>
            // layer 1 upper
            // layer 2 upper
            layers[0] = new PanoramaMapViewDrawLayer(img);
            layers[1] = new R2kTileMapViewDrawLayer(tbl, 0, false, tileset);
            layers[2] = new R2kTileMapViewDrawLayer(tbl, 1, false, tileset);
            layers[3] = new EventMapViewDrawLayer(0, events, eventRenderer);
            layers[4] = new R2kTileMapViewDrawLayer(tbl, 0, true, tileset);
            layers[5] = new R2kTileMapViewDrawLayer(tbl, 1, true, tileset);
            layers[6] = new EventMapViewDrawLayer(1, events, eventRenderer);
            layers[7] = new EventMapViewDrawLayer(2, events, eventRenderer);
        }
        return new StuffRenderer(imageLoader, tileRenderer, eventRenderer, layers);
    }

    @Override
    public StuffRenderer rendererFromTso(RubyIO tso) {
        IImageLoader imageLoader = new CacheImageLoader(new XYZOrPNGImageLoader(AppMain.rootPath));
        ITileRenderer tileRenderer = new LcfTileRenderer(imageLoader, tso);
        IEventGraphicRenderer eventRenderer = new R2kEventGraphicRenderer(imageLoader, tileRenderer);
        return new StuffRenderer(imageLoader, tileRenderer, eventRenderer, new IMapViewDrawLayer[0]);
    }
}