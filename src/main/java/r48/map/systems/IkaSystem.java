/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package r48.map.systems;

import r48.AppMain;
import r48.RubyIO;
import r48.map.StuffRenderer;
import r48.map.events.IEventGraphicRenderer;
import r48.map.events.IkaEventGraphicRenderer;
import r48.map.imaging.CacheImageLoader;
import r48.map.imaging.GabienImageLoader;
import r48.map.tiles.ITileRenderer;
import r48.map.tiles.IkaTileRenderer;

/**
 * Created on 03/06/17.
 */
public class IkaSystem extends MapSystem {
    public IkaSystem() {
        super(new CacheImageLoader(new GabienImageLoader(AppMain.rootPath + "Pbm/", ".pbm", 0, 0, 0)));
    }

    @Override
    public StuffRenderer rendererFromMap(RubyIO map) {
        ITileRenderer tileRenderer = new IkaTileRenderer(imageLoader);
        IEventGraphicRenderer eventRenderer = new IkaEventGraphicRenderer(imageLoader);
        return new StuffRenderer(imageLoader, tileRenderer, eventRenderer, StuffRenderer.prepareTraditional(tileRenderer, new int[] {0}, eventRenderer, imageLoader, map, "Back", true, true, 0, 0, 0, 0, 1));
    }
}
