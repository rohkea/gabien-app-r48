/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.io.r2k.struct;

import r48.RubyIO;
import r48.io.IntUtils;
import r48.io.data.IRIO;
import r48.io.data.IRIOFixed;
import r48.io.r2k.R2kUtil;
import r48.io.r2k.chunks.IR2kStruct;
import r48.io.r2k.chunks.StringR2kStruct;

import java.io.*;

/**
 * "COPY jun6-2017" I have no idea what that means.
 * Coming back to this on December 6th 2018, this is one annoying class to deal with.
 * For these reasons and more, I'm pretending it's a fixed-size struct.
 */
public class Terms extends IRIOFixed implements IR2kStruct {
    public static int[] mapping = {
            0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A,
            0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10, 0x11, 0x12, 0x13, 0x14,
            0x15, 0x16, 0x17, 0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E,
            0x1F, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27,
            0x29, 0x2A, 0x2B, 0x2C, 0x2D, 0x2E, 0x2F, 0x30, 0x31, 0x32, 0x33,
            0x36, 0x37, 0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F, 0x40,
            0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4A, 0x4B, 0x4C, 0x4D,
            0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59,
            0x5C, 0x5D,
            0x5F,
            0x65, 0x66, 0x67, 0x68, 0x69, 0x6A, 0x6B, 0x6C,
            0x6E,
            0x70,
            0x72, 0x73,
            0x75, 0x76, 0x77, 0x78, 0x79, 0x7A, 0x7B, 0x7C, 0x7D, 0x7E, 0x7F,
            0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8A, 0x8B, 0x8C,
            0x92, 0x93, 0x94,
            0x97, 0x98, 0x99
    };

    // This is MASSIVE. Luckily it's also 100% strings?
    // 380 to 506 incl.
    // 506 - 380 == 127
    public StringR2kStruct[] termArray = new StringR2kStruct[mapping.length];

    public Terms() {
        super('[');
        setArray();
    }

    @Override
    public IRIO setArray() {
        for (int i = 0; i < termArray.length; i++)
            termArray[i] = new StringR2kStruct();
        return this;
    }

    @Override
    public int getALen() {
        return termArray.length;
    }

    @Override
    public IRIO getAElem(int i) {
        return termArray[i];
    }

    @Override
    public boolean getAFixedFormat() {
        return true;
    }

    @Override
    public RubyIO asRIO() {
        return new RubyIO().setDeepClone(this);
    }

    @Override
    public void fromRIO(IRIO src) {
        setDeepClone(src);
    }

    @Override
    public String[] getIVars() {
        return new String[0];
    }

    @Override
    public IRIO addIVar(String sym) {
        return null;
    }

    @Override
    public IRIO getIVar(String sym) {
        return null;
    }

    @Override
    public void importData(InputStream bais) throws IOException {
        for (int i = 0; i < termArray.length; i++)
            termArray[i] = new StringR2kStruct();
        while (true) {
            int idx = R2kUtil.readLcfVLI(bais);
            if (idx == 0)
                break;
            int len = R2kUtil.readLcfVLI(bais);
            byte[] data = IntUtils.readBytes(bais, len);
            for (int i = 0; i < mapping.length; i++) {
                if (mapping[i] == idx) {
                    termArray[i].importData(new ByteArrayInputStream(data));
                    break;
                }
            }
        }
    }

    @Override
    public boolean exportData(OutputStream baos) throws IOException {
        for (int i = 0; i < termArray.length; i++) {
            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
            termArray[i].exportData(baos2);
            byte[] data = baos2.toByteArray();
            R2kUtil.writeLcfVLI(baos, mapping[i]);
            R2kUtil.writeLcfVLI(baos, data.length);
            baos.write(data);
        }
        baos.write(0);
        return false;
    }
}