/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.io.r2k.dm2chk;

import r48.RubyIO;
import r48.io.IntUtils;
import r48.io.data.*;
import r48.io.r2k.R2kUtil;
import r48.io.r2k.chunks.IR2kInterpretable;
import r48.io.r2k.chunks.IR2kStruct;
import r48.io.r2k.chunks.IntegerR2kStruct;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * How this works:
 * It's in two states (decided by unpackedDataValid)
 * false: Packed. All fields are INVALID, apart from packedChunkData.
 * true: Unpacked. Fields become valid, packedChunkData nulled.
 * DM2LcfBinding must be present on all fields that act as serializable chunks.
 * Reinitialization of
 * Modified from R2kObject on December 4th 2018.
 */
public class DM2R2kObject extends IRIOFixedObject implements IR2kStruct {
    @DM2FXOBinding(optional = true, iVar = "@__LCF__unknown")
    public IRIOFixedHash<Integer, IRIOFixedUser> unknownChunks;

    // --

    // This data becomes potentially out of date the moment the object is unpacked, so this should be nulled then.
    // A note here:
    private HashMap<Integer, byte[]> packedChunkData = new HashMap<Integer, byte[]>();

    public DM2R2kObject(String sym) {
        super(sym);
    }

    @Override
    protected final void initialize() {
        // Disable automatic initialization, permanently
    }

    protected IR2kInterpretable dm2ReinitializeBound(Field f, boolean present) {
        IRIO res = trySetFXOChunk(f, present);
        if (res != null)
            return (IR2kInterpretable) res;
        try {
            return (IR2kInterpretable) f.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private IRIO trySetFXOChunk(Field f, boolean present) {
        DM2LcfInteger fxi = f.getAnnotation(DM2LcfInteger.class);
        if (fxi != null) {
            try {
                IntegerR2kStruct i = new IntegerR2kStruct(fxi.value());
                f.set(this, i);
                return i;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        DM2FXOBinding fxo = f.getAnnotation(DM2FXOBinding.class);
        if (fxo != null)
            if (present || !fxo.optional())
                return addIVar(fxo.iVar());
        return null;
    }

    private void setUnknownChunks() {
        unknownChunks = new IRIOFixedHash<Integer, IRIOFixedUser>() {
            @Override
            public Integer convertIRIOtoKey(IRIO i) {
                return (int) i.getFX();
            }

            @Override
            public IRIO convertKeyToIRIO(Integer i) {
                return new IRIOFixnum(i);
            }

            @Override
            public IRIOFixedUser newValue() {
                return new IRIOFixedUser("Blob", new byte[0]);
            }
        };
    }

    // Packed data management

    // May actually return packedChunkData.
    protected HashMap<Integer, byte[]> dm2Pack() throws IOException {
        if (packedChunkData != null)
            return packedChunkData;
        HashMap<Integer, byte[]> ws = new HashMap<Integer, byte[]>();
        dm2PackIntoMap(ws);
        if (unknownChunks != null)
            for (Map.Entry<Integer, IRIOFixedUser> uv : unknownChunks.hashVal.entrySet())
                ws.put(uv.getKey(), uv.getValue().userVal);
        return ws;
    }

    protected void dm2Unpack() {
        if (packedChunkData != null) {
            HashMap<Integer, byte[]> pcd = packedChunkData;
            packedChunkData = null;
            unknownChunks = null;
            // Removes chunks as they are found so that unknown chunks can eat the rest
            dm2UnpackFromMapDestructively(pcd);
            if (pcd.size() > 0) {
                setUnknownChunks();
                for (Map.Entry<Integer, byte[]> me : pcd.entrySet())
                    unknownChunks.hashVal.put(me.getKey(), new IRIOFixedUser("Blob", me.getValue()));
            }
        }
    }

    protected void dm2PackIntoMap(HashMap<Integer, byte[]> pcd) throws IOException {
        for (Field f : cachedFields) {
            DM2LcfBinding dlb = f.getAnnotation(DM2LcfBinding.class);
            if (dlb != null) {
                IR2kInterpretable iri;
                try {
                    iri = (IR2kInterpretable) f.get(this);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
                if (iri != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    if (!iri.exportData(baos))
                        pcd.put(dlb.index(), baos.toByteArray());
                }
            }
        }
    }

    protected void dm2UnpackFromMapDestructively(HashMap<Integer, byte[]> pcd) {
        for (Field f : cachedFields) {
            // Null all fields by default,
            //  but if they have an LCF or FXO binding try to revive them
            try {
                f.set(this, null);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            DM2LcfBinding dlb = f.getAnnotation(DM2LcfBinding.class);

            if (dlb != null) {
                byte[] relevantChunk = pcd.remove(dlb.index());
                IR2kInterpretable iri = dm2ReinitializeBound(f, relevantChunk != null);
                if (relevantChunk != null) {
                    try {
                        if (iri == null)
                            throw new RuntimeException("Chunk " + dlb.index() + " ( " + f + " ) wasn't created on time");
                        ByteArrayInputStream bais = new ByteArrayInputStream(relevantChunk);
                        iri.importData(bais);
                        if (bais.available() > 0)
                            if (disableSanity())
                                throw new RuntimeException("Chunk " + dlb.index() + " ( " + f + " ) had too much data");
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                }
            } else {
                trySetFXOChunk(f, false);
            }
        }
    }

    // --- Actual IRIO impl.

    @Override
    public IRIO setObject(String symbol) {
        super.setObject(symbol);
        // Switch this into unpacked by force. Deletion of all data is totally okay here.
        packedChunkData = null;
        // unknownChunks & main members (since normal initialize() disabled)
        // Any remaining stuff 'should be fine'
        super.initialize();
        return this;
    }

    @Override
    public final String[] getIVars() {
        dm2Unpack();
        return dm2GetIVars();
    }

    @Override
    public final IRIO getIVar(String sym) {
        dm2Unpack();
        return dm2GetIVar(sym);
    }

    @Override
    public final IRIO addIVar(String sym) {
        dm2Unpack();
        if (sym.equals("@__LCF__unknown")) {
            setUnknownChunks();
            return unknownChunks;
        }
        return dm2AddIVar(sym);
    }

    @Override
    public void rmIVar(String sym) {
        dm2Unpack();
        dm2RmIVar(sym);
    }

    // --- Override These!

    protected String[] dm2GetIVars() {
        return super.getIVars();
    }

    protected IRIO dm2GetIVar(String sym) {
        return super.getIVar(sym);
    }

    protected IRIO dm2AddIVar(String sym) {
        return null;
    }

    protected void dm2RmIVar(String sym) {
        super.rmIVar(sym);
    }

    // ---

    // Equivalent of liblcf's conditional_zero.
    // If true, then no zero is required and no zero is written.
    protected boolean terminatable() {
        return false;
    }

    // If enabled, it's okay for the interpretable for a chunk to not read 100% of the chunk.
    protected boolean disableSanity() {
        return false;
    }

    // ---

    public void importData(InputStream src) throws IOException {
        packedChunkData = new HashMap<Integer, byte[]>();
        while (true) {
            if (src.available() == 0)
                if (terminatable())
                    break;
            int cid = R2kUtil.readLcfVLI(src);
            if (cid == 0)
                break;
            int len = R2kUtil.readLcfVLI(src);
            // System.out.println(this + " -> 0x" + Integer.toHexString(cid) + " [" + len + "]");
            byte[] data = IntUtils.readBytes(src, len);
            packedChunkData.put(cid, data);
            // System.out.println("<<");
        }
    }

    public boolean exportData(OutputStream baos2) throws IOException {
        HashMap<Integer, byte[]> packed = dm2Pack();
        // Collate all chunks
        LinkedList<Integer> keys = new LinkedList<Integer>(packed.keySet());
        Collections.sort(keys);
        for (Integer i : keys) {
            byte[] data = packed.get(i);
            R2kUtil.writeLcfVLI(baos2, i);
            R2kUtil.writeLcfVLI(baos2, data.length);
            baos2.write(data);
        }
        if (!terminatable())
            baos2.write(0);
        return false;
    }

    @Override
    public RubyIO asRIO() {
        return new RubyIO().setDeepClone(this);
    }

    @Override
    public void fromRIO(IRIO src) {
        setDeepClone(src);
    }
}