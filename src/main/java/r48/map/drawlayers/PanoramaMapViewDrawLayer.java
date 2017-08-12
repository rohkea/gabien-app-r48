/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package r48.map.drawlayers;

import gabien.GaBIEn;
import gabien.IGrDriver;
import gabien.IGrInDriver;
import gabien.IImage;
import gabien.ui.UIElement;
import r48.dbs.TXDB;
import r48.map.IMapViewCallbacks;

/**
 * Used for drawing panoramas.
 * Created on 08/06/17.
 */
public class PanoramaMapViewDrawLayer implements IMapViewDrawLayer {
    private final IImage im;
    private boolean loopX, loopY;
    private int autoLoopX, autoLoopY;

    public PanoramaMapViewDrawLayer(IImage pano, boolean lx, boolean ly, int alx, int aly) {
        im = pano;
        loopX = lx;
        loopY = ly;
        autoLoopX = alx;
        autoLoopY = aly;
    }

    @Override
    public String getName() {
        return TXDB.get("Panorama");
    }

    public void draw(int camX, int camY, int camTX, int camTY, int camTR, int camTB, int mouseXT, int mouseYT, int eTileSize, int currentLayer, IMapViewCallbacks callbacks, boolean debug, IGrDriver igd) {
        // Panorama Enable
        if (im != null) {
            // Need to tile the area with the image.
            // I give up, this is what I've got now.
            // It works better this way than the other way under some cases.

            int eCamX = camX;
            int eCamY = camY;

            // ... later:
            // The basis of parallax appears to be "whatever the camera was set to beforehand"
            // For accurate results despite this "varying basis",
            //  emulation needs to get the difference between R48's camera and an idealized 20x15 camera @ the top-left.
            // For animated parallax, it takes 40 seconds for a value of 1 to travel 160 pixels (tested on RPG_RT)
            // This boils down to precisely 4 pixels per second per speed value.
            int centreX = eTileSize * 10;
            int centreY = (eTileSize * 15) / 2;
            int cxc = camX + (igd.getWidth() / 2);
            int cyc = camY + (igd.getHeight() / 2);

            if (loopX)
                eCamX -= ((cxc - centreX) / 2) + ((int) (autoLoopX * 4 * GaBIEn.getTime()));
            if (loopY)
                eCamY -= ((cyc - centreY) / 2) + ((int) (autoLoopY * 4 * GaBIEn.getTime()));

            int camOTX = UIElement.sensibleCellDiv(eCamX, im.getWidth());
            int camOTY = UIElement.sensibleCellDiv(eCamY, im.getHeight());
            int camOTeX = UIElement.sensibleCellDiv(eCamX + igd.getWidth(), im.getWidth()) + 1;
            int camOTeY = UIElement.sensibleCellDiv(eCamY + igd.getHeight(), im.getHeight()) + 1;

            // If *nothing's* looping, it's probably 'bound to the map' (YumeNikki Nexus, OneShot Maize).
            // Failing anything else this helps avoid confusion: "where was the actual map again?"
            // (PARTICULARLY helps with YumeNikki igloos, but really, the whole thing *just works well*)
            if (!(loopX || loopY)) {
                camOTX = 0;
                camOTeX = 0;
                camOTY = 0;
                camOTeY = 0;
            }

            for (int i = camOTX; i <= camOTeX; i++)
                for (int j = camOTY; j <= camOTeY; j++)
                    igd.blitImage(0, 0, im.getWidth(), im.getHeight(), (i * im.getWidth()) - eCamX, (j * im.getHeight()) - eCamY, im);
        }
    }
}
