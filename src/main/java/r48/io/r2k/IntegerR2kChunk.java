/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package r48.io.r2k;

import java.io.IOException;

/**
 * yay, an integer!
 * Created on 31/05/17.
 */
public class IntegerR2kChunk implements IR2kProp {
    public int i;

    public IntegerR2kChunk(int i2) {
        i = i2;
    }

    @Override
    public byte[] getData() {
        throw new RuntimeException("incomplete");
    }

    @Override
    public void importData(byte[] data) throws IOException {
        i = R2kUtil.readLcfVLI(data);
    }
}
