/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package r48.io.r2k;

import r48.RubyIO;

import java.io.IOException;
import java.io.InputStream;

/**
 * Just a namespace-like for map IO functions.
 *
 * NOTES ABOUT ALL THIS STUFF
 * Ok, so basically, the map format is designed with properties in mind.
 * You know, similar to the Ruby format in that they can omit properties.
 * However, properties are referred to by object-type-specific indexes.
 * Also, these properties are, AFAIK, always in order.
 * The "end property" has an index of 0.
 *
 * I would assume this applies to the other formats as well.
 *
 * Created on 30/05/17.
 */
public class MapIO {
    public static RubyIO readLmu(InputStream fis) throws IOException {
        String magic = R2kUtil.decodeLcfString(R2kUtil.readLcfBytes(fis, R2kUtil.readLcfVLI(fis)));
        if (!magic.equals("LcfMapUnit"))
            throw new IOException("Not an LcfMapUnit");
        // Try to follow the standard...
        MapUnit mu = new MapUnit();
        R2kUtil.readLcfObj(mu.indices, mu.unknownChunks, fis);
        return mu.asRIO();
    }

}
