/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.map.systems;

import gabien.GaBIEn;
import gabien.IGrDriver;
import gabien.IImage;
import gabien.ui.*;
import r48.*;
import r48.dbs.TSDB;
import r48.dbs.TXDB;
import r48.io.PathUtils;
import r48.map.IEditingToolbarController;
import r48.map.IMapToolContext;
import r48.map.MapEditingToolbarController;
import r48.map.StuffRenderer;
import r48.map.drawlayers.*;
import r48.map.events.IEventGraphicRenderer;
import r48.map.events.TraditionalEventAccess;
import r48.map.imaging.*;
import r48.map.tiles.GenericTileRenderer;
import r48.map.tiles.ITileRenderer;
import r48.map.tiles.IndirectTileRenderer;
import r48.map.tiles.NullTileRenderer;

/**
 * It's a secret to everybody.
 * Created on 11th May 2018
 */
public class CSOSystem extends MapSystem {
    public CSOSystem() {
        super(new CacheImageLoader(new FixAndSecondaryImageLoader("", "", new ChainedImageLoader(new IImageLoader[] {
                // PNGs are NOT interpreted via PNG8I, ever
                new GabienImageLoader(".png")
        }))), true);
    }

    @Override
    public UIElement createMapExplorer(IConsumer<UIElement> windowMaker, final IMapContext mapBox, String mapInfos) {
        final UIScrollLayout usl = new UIScrollLayout(true, FontSizes.generalScrollersize) {
            @Override
            public String toString() {
                return TXDB.get("Map List");
            }
        };
        final Runnable refresh = new Runnable() {
            @Override
            public void run() {
                usl.panelsClear();
                for (String gamemode : GaBIEn.listEntries(PathUtils.autoDetectWindows(AppMain.rootPath + "stages"))) {
                    String adw = PathUtils.autoDetectWindows(AppMain.rootPath + "stages/" + gamemode);
                    if (GaBIEn.dirExists(adw)) {
                        for (String map : GaBIEn.listEntries(adw)) {
                            if (map.toLowerCase().endsWith(".pxm")) {
                                final String mapFinale = gamemode + "/" + map.substring(0, map.length() - 4);
                                usl.panelsAdd(new UITextButton(mapFinale, FontSizes.mapInfosTextHeight, new Runnable() {
                                    @Override
                                    public void run() {
                                        mapBox.loadMap(mapFinale);
                                    }
                                }));
                            }
                        }
                    }
                }
            }
        };
        refresh.run();
        return usl;
    }

    @Override
    public StuffRenderer rendererFromTso(RubyIO target) {
        IImage pano = GaBIEn.getErrorImage();
        IMapViewDrawLayer[] layers = new IMapViewDrawLayer[0];
        ITileRenderer tr = new NullTileRenderer();
        final IImage quote = GaBIEn.getImageEx("CSO/quote.png", false, true);
        final IImage tiles = GaBIEn.getImageEx("CSO/tiles.png", false, true);
        IEventGraphicRenderer ev = new IEventGraphicRenderer() {
            @Override
            public int determineEventLayer(RubyIO event) {
                return 0;
            }

            @Override
            public RubyIO extractEventGraphic(RubyIO event) {
                return event;
            }

            @Override
            public void drawEventGraphic(RubyIO target, int ox, int oy, IGrDriver igd, int sprScale) {
                int scx = 0;
                if (target.getInstVarBySymbol("@type").fixnumVal < 0)
                    scx = 1;
                igd.blitScaledImage(scx * 16, 0, 16, 16, ox, oy, 16 * sprScale, 16 * sprScale, quote);
            }
        };
        if (target != null) {
            // Target might be the map, which is used as a TSO for the PXA access.
            if (target.type != '"')
                target = new RubyIO().setString(AppMain.objectDB.getIdByObject(target), true);
        }
        if (target != null) {
            String str = target.decString();
            pano = imageLoader.getImage(AppMain.dataPath + str + "BG", true);
            tr = new GenericTileRenderer(imageLoader.getImage(AppMain.dataPath + str, true), 16, 16, 256);
            RubyIO target2 = AppMain.objectDB.getObject(str);
            TraditionalEventAccess tea = new TraditionalEventAccess(target2, "@psp", 0, "SPEvent");
            // biscuits are not available in this build.
            RubyTable pxmTab = new RubyTable(target2.getInstVarBySymbol("@pxm").userVal);
            RubyTable pxaTab = new RubyTable(target2.getInstVarBySymbol("@pxa").userVal);
            layers = new IMapViewDrawLayer[] {
                    new PanoramaMapViewDrawLayer(pano, true, true, 0, 0, 0, 0, 0, 0, 1),
                    new TileMapViewDrawLayer(pxmTab, 0, tr),
                    new TileMapViewDrawLayer(pxmTab, 0, new IndirectTileRenderer(pxaTab, new GenericTileRenderer(new TSDB("CSO/TileInfo.txt").compileSheet(256, 16), 16, 256, 256))),
                    new EventMapViewDrawLayer(0, tea, ev, 16),
                    new EventMapViewDrawLayer(0x7FFFFFFF, tea, ev, 16),
                    new GridMapViewDrawLayer()
            };
        }
        return new StuffRenderer(imageLoader, tr, ev, layers);
    }

    @Override
    public MapViewDetails mapViewRequest(final String gum, boolean allowCreate) {
        return new MapViewDetails(gum, "CSOMap", new IFunction<String, MapViewState>() {
            @Override
            public MapViewState apply(String s) {
                final RubyIO mapRIO = AppMain.objectDB.getObject(gum);
                return MapViewState.fromRT(rendererFromTso(new RubyIO().setString(gum, true)), gum, new String[] {}, mapRIO, "@pxm", false, new TraditionalEventAccess(mapRIO, "@psp", 0, "SPEvent"));
            }
        }, new IFunction<IMapToolContext, IEditingToolbarController>() {
            @Override
            public IEditingToolbarController apply(IMapToolContext iMapToolContext) {
                return new MapEditingToolbarController(iMapToolContext, false);
            }
        });
    }
}
