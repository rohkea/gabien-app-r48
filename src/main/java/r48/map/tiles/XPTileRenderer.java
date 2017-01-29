/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package r48.map.tiles;

import gabien.GaBIEn;
import gabien.IGrInDriver;
import gabien.ui.UILabel;
import r48.AppMain;
import r48.FontSizes;
import r48.RubyIO;
import r48.dbs.ATDB;
import r48.map.UIMapView;
import r48.ui.UITileGrid;

/**
 *
 * Created on 1/27/17.
 */
public class XPTileRenderer implements ITileRenderer {
    private final RubyIO tileset;
    public final IGrInDriver.IImage[] tilesetMaps = new IGrInDriver.IImage[8];

    public static final int tileSize = 32;

    @Override
    public int getTileSize() {
        return tileSize;
    }

    public XPTileRenderer(RubyIO tileset) {
        this.tileset = tileset;
        // If the tileset's null, then just give up.
        // The tileset being/not being null is an implementation detail anyway.
        if (tileset != null) {
            RubyIO tn = tileset.getInstVarBySymbol("@tileset_name");
            if (tn != null) {
                // XP
                String expectedTS = tn.decString();
                if (expectedTS.length() != 0)
                    tilesetMaps[0] = GaBIEn.getImage(AppMain.rootPath + "Graphics/Tilesets/" + expectedTS + ".png", 0, 0, 0);
                RubyIO[] amNames = tileset.getInstVarBySymbol("@autotile_names").arrVal;
                for (int i = 0; i < 7; i++) {
                    RubyIO rio = amNames[i];
                    if (rio.strVal.length != 0) {
                        String expectedAT = rio.decString();
                        tilesetMaps[i + 1] = GaBIEn.getImage(AppMain.rootPath + "Graphics/Autotiles/" + expectedAT + ".png", 0, 0, 0);
                    }
                }
            }
        }
    }

    @Override
    public void drawTile(int layer, short tidx, int px, int py, IGrInDriver igd, int ets) {
        // The logic here is only documented in the mkxp repository, in tilemap.cpp.
        // I really hope it doesn't count as stealing here,
        //  if I would've had to have typed this code ANYWAY
        //  after an age trying to figure it out.
        if (tidx < (48 * 8)) {
            // Autotile
            int atMap = tidx / 48;
            if (atMap == 0)
                return;
            tidx %= 48;
            boolean didDraw = false;
            if (tilesetMaps[atMap] != null) {
                if (ets == tileSize) {
                    ATDB.Autotile at = AppMain.autoTiles.entries[tidx];
                    if (at != null){
                        int cSize = tileSize / 2;
                        for (int sA = 0; sA < 2; sA++)
                            for (int sB = 0; sB < 2; sB++) {
                                int ti = at.corners[sA + (sB * 2)];
                                int tx = ti % 3;
                                int ty = ti / 3;
                                int sX = (sA * cSize);
                                int sY = (sB * cSize);
                                igd.blitImage((tx * tileSize) + sX, (ty * tileSize) + sY, cSize, cSize, px + sX, py + sY, tilesetMaps[atMap]);
                            }
                        didDraw = true;
                    }
                } else {
                    igd.blitImage(tileSize, 2 * tileSize, ets, ets, px, py, tilesetMaps[atMap]);
                    didDraw = true; // Close enough
                }
            } else {
                didDraw = true; // It's invisible, so it should just be considered drawn no matter what
            }
            if (!didDraw)
                UILabel.drawString(igd, px, py, ":" + tidx, false, FontSizes.mapDebugTextHeight);
            return;
        }
        tidx -= 48 * 8;
        int tsh = 8;
        int tx = tidx % tsh;
        int ty = tidx / tsh;
        if (tilesetMaps[0] != null)
            igd.blitImage(tx * tileSize, ty * tileSize, ets, ets, px, py, tilesetMaps[0]);
    }

    @Override
    public String getPanorama() {
        if (tileset != null) {
            RubyIO rio = tileset.getInstVarBySymbol("@panorama_name");
            if (rio != null)
                if (rio.strVal.length > 0)
                    return "Graphics/Panoramas/" + rio.decString() + ".png";
        }
        return "";
    }

    @Override
    public UITileGrid[] createATUIPlanes(UIMapView mv) {
        IGrInDriver.IImage tm0 = tilesetMaps[0];
        int tileCount = 48;
        if (tm0 != null)
            tileCount = ((tm0.getHeight() / 32) * 8);
        return new UITileGrid[] {
                new UITileGrid(mv, 0, 49, true),
                new UITileGrid(mv, 48, 49, true),
                new UITileGrid(mv, 48 * 2, 49, true),
                new UITileGrid(mv, 48 * 3, 49, true),
                new UITileGrid(mv, 48 * 4, 49, true),
                new UITileGrid(mv, 48 * 5, 49, true),
                new UITileGrid(mv, 48 * 6, 49, true),
                new UITileGrid(mv, 48 * 7, 49, true),
                new UITileGrid(mv, 48 * 8, tileCount, false),
        };
    }

    @Override
    public String[] getPlaneNames() {
        return new String[] {
                "NULL",
                "A1",
                "A2",
                "A3",
                "A4",
                "A5",
                "A6",
                "A7",
                "TM"
        };
    }

    @Override
    public int[] indicateATs() {
        return new int[] {
            0,
            48,
            48 * 2,
            48 * 3,
            48 * 4,
            48 * 5,
            48 * 6,
            48 * 7
        };
    }
}