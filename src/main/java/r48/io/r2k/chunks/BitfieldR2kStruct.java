/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package r48.io.r2k.chunks;

import r48.RubyIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created on 02/06/17.
 */
public class BitfieldR2kStruct extends IntegerR2kStruct {

    // Ascending
    public final String[] flags;

    public BitfieldR2kStruct(String[] f) {
        super(0);
        flags = f;
    }

    @Override
    public RubyIO asRIO() {
        RubyIO r = new RubyIO().setSymlike("__bitfield__", true);
        int pwr = 1;
        for (String s : flags) {
            r.iVars.put(s, new RubyIO().setBool((pwr & i) != 0));
            pwr <<= 1;
        }
        return r;
    }

    @Override
    public void fromRIO(RubyIO src) {
        int pwr = 1;
        for (String s : flags) {
            if (src.getInstVarBySymbol(s).type == 'T')
                i |= pwr;
            pwr <<= 1;
        }
    }
}
