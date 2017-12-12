/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.io.r2k.chunks;

import r48.RubyIO;
import r48.io.r2k.R2kUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created on 05/06/17.
 */
public class ByteR2kStruct implements IR2kStruct {
    public byte value;
    public boolean signed = false;

    public ByteR2kStruct(int v) {
        value = (byte) v;
    }

    public ByteR2kStruct signed() {
        signed = true;
        return this;
    }

    @Override
    public RubyIO asRIO() {
        if (signed)
            return new RubyIO().setFX(value);
        return new RubyIO().setFX(value & 0xFF);
    }

    @Override
    public void fromRIO(RubyIO src) {
        value = (byte) (src.fixnumVal);
    }

    @Override
    public void importData(InputStream bais) throws IOException {
        value = (byte) R2kUtil.readLcfU8(bais);
    }

    @Override
    public boolean exportData(OutputStream baos) throws IOException {
        baos.write(value);
        return false;
    }
}
