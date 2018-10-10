/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.schema.specialized.genpos;

import r48.RubyTable;

/**
 * Created on October 10, 2018.
 */
public class TableGenposTweeningProp implements IGenposTweeningProp {
    public final RubyTable rt;
    public final int x, y, z;

    public TableGenposTweeningProp(RubyTable rt, int x, int y, int z) {
        this.rt = rt;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public double getValue() {
        return rt.getTiletype(x, y, z);
    }

    @Override
    public void setValue(double value) {
        rt.setTiletype(x, y, z, (short) value);
    }

    @Override
    public boolean round() {
        return true;
    }
}
