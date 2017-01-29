/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package r48.map.events;

import gabien.IGrInDriver;
import r48.RubyIO;

/**
 * Events need to have a @x, and a @y, (@name is optional) but anything else about them is determined by this and the Schema.
 * Created on 1/27/17.
 */
public interface IEventGraphicRenderer {
    RubyIO extractEventGraphic(RubyIO event);
    void drawEventGraphic(RubyIO target, int ox, int oy, IGrInDriver igd);
}