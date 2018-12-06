/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.io.r2k.obj.ldb;

import gabien.ui.ISupplier;
import r48.io.data.DM2FXOBinding;
import r48.io.data.IRIO;
import r48.io.r2k.dm2chk.DM2LcfBinding;
import r48.io.r2k.dm2chk.DM2R2kObject;
import r48.io.r2k.dm2chk.DM2SparseArrayA;

/**
 * Created on 07/06/17.
 */
public class AnimationFrame extends DM2R2kObject {
    @DM2FXOBinding("@cells") @DM2LcfBinding(1)
    public DM2SparseArrayA<AnimationCell> cells;

    public AnimationFrame() {
        super("RPG::Animation::Frame");
    }

    @Override
    protected IRIO dm2AddIVar(String sym) {
        if (sym.equals("@cells"))
            return cells = new DM2SparseArrayA<AnimationCell>(new ISupplier<AnimationCell>() {
                @Override
                public AnimationCell get() {
                    return new AnimationCell();
                }
            });
        return super.dm2AddIVar(sym);
    }
}
