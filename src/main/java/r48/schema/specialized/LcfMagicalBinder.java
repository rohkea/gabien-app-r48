/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.schema.specialized;

import gabien.ui.ISupplier;
import r48.RubyIO;
import r48.io.data.IRIO;
import r48.io.r2k.chunks.IR2kStruct;
import r48.io.r2k.chunks.SparseArrayAR2kStruct;
import r48.io.r2k.chunks.SparseArrayHR2kStruct;
import r48.io.r2k.obj.ldb.AnimationFrame;
import r48.io.r2k.obj.ldb.BAD;
import r48.io.r2k.obj.ldb.Troop;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Solves the FT3 memory problem by cutting the branches.
 * Created on February 10th 2018.
 */
public class LcfMagicalBinder implements IMagicalBinder {
    private final ISupplier<IR2kStruct> inner;
    private final String className;
    public LcfMagicalBinder(String cn, ISupplier<IR2kStruct> iSupplier) {
        inner = iSupplier;
        className = cn;
    }

    protected static IMagicalBinder getTroopPages(String binderPrefix) {
        return new LcfMagicalBinder(binderPrefix + "R2kTroopPages", new ISupplier<IR2kStruct>() {
            @Override
            public IR2kStruct get() {
                return new SparseArrayAR2kStruct<Troop.TroopPage>(new ISupplier<Troop.TroopPage>() {
                    @Override
                    public Troop.TroopPage get() {
                        return new Troop.TroopPage();
                    }
                });
            }
        });
    }

    protected static IMagicalBinder getAnimationFrames(String binderPrefix) {
        return new LcfMagicalBinder(binderPrefix + "R2kAnimationFrames", new ISupplier<IR2kStruct>() {
            @Override
            public IR2kStruct get() {
                return new SparseArrayAR2kStruct<AnimationFrame>(new ISupplier<AnimationFrame>() {
                    @Override
                    public AnimationFrame get() {
                        return new AnimationFrame();
                    }
                });
            }
        });
    }

    protected static IMagicalBinder getBattlerAnimationMap(String binderPrefix) {
        return new LcfMagicalBinder(binderPrefix + "R2kBattlerAnimationMap", new ISupplier<IR2kStruct>() {
            @Override
            public IR2kStruct get() {
                return new SparseArrayHR2kStruct<BAD>(new ISupplier<BAD>() {
                    @Override
                    public BAD get() {
                        return new BAD();
                    }
                });
            }
        });
    }

    @Override
    public RubyIO targetToBoundNCache(IRIO target) {
        IR2kStruct s = inner.get();
        try {
            s.importData(new ByteArrayInputStream(target.getBuffer()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return s.asRIO();
    }

    @Override
    public boolean applyBoundToTarget(IRIO bound, IRIO target) {
        IR2kStruct s = inner.get();
        s.fromRIO(bound);
        byte[] tba = getStructBytes(s);
        // Try to ensure target is a blob.
        boolean setDefault = target.getType() != 'u';
        if (!setDefault)
            setDefault = !target.getSymbol().equals(className);
        if (setDefault) {
            target.setUser(className, tba);
            return true;
        } else {
            byte[] tb = target.getBuffer();
            if (tb.length == tba.length) {
                boolean same = true;
                for (int i = 0; i < tba.length; i++) {
                    if (tba[i] != tb[i]) {
                        same = false;
                        break;
                    }
                }
                if (same)
                    return false;
            }
        }
        target.putBuffer(tba);
        return true;
    }

    private byte[] getStructBytes(IR2kStruct s) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            s.exportData(baos);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return baos.toByteArray();
    }

    @Override
    public boolean modifyVal(IRIO trueTarget, boolean setDefault) {
        boolean mod = setDefault || (trueTarget.getType() != 'u');
        // It's 'u', but check class
        if (!mod)
            mod = !trueTarget.getSymbol().equals(className);
        if (mod) {
            // This sets up the structure.
            trueTarget.setUser(className, getStructBytes(inner.get()));
        }
        return mod;
    }
}
