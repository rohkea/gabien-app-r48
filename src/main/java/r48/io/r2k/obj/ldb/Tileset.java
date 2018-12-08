/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.io.r2k.obj.ldb;

import r48.RubyTable;
import r48.io.data.DM2FXOBinding;
import r48.io.data.IRIO;
import r48.io.r2k.R2kUtil;
import r48.io.r2k.chunks.BlobR2kStruct;
import r48.io.r2k.chunks.BooleanR2kStruct;
import r48.io.r2k.chunks.IntegerR2kStruct;
import r48.io.r2k.chunks.StringR2kStruct;
import r48.io.r2k.dm2chk.*;

import java.io.IOException;
import java.util.HashMap;

/**
 * Another bare-minimum for now
 * Created on 01/06/17.
 */
public class Tileset extends DM2R2kObject {
    @DM2FXOBinding("@name") @DM2LcfBinding(0x01) @DM2LcfObject
    public StringR2kStruct name;
    @DM2FXOBinding("@tileset_name") @DM2LcfBinding(0x02) @DM2LcfObject
    public StringR2kStruct tilesetName;
    // Tables? Tables. Don't need to put these as optional explicitly because not Index-based.
    // ...and anyway, I get the feeling they aren't actually SUPPOSED to be optional.
    // excuse me while I go mess around with the schema.
    // -- AS OF DM2, some blob transformation occurs similar to SaveMapInfo.
    @DM2FXOBinding("@terrain_id_data") @DM2LcfBinding(3)
    public BlobR2kStruct terrainTbl;
    @DM2FXOBinding("@lowpass_data") @DM2LcfBinding(4)
    public BlobR2kStruct lowPassTbl;
    @DM2FXOBinding("@highpass_data") @DM2LcfBinding(5)
    public BlobR2kStruct highPassTbl;

    @DM2FXOBinding("@anim_cyclic") @DM2LcfBinding(0x0B) @DM2LcfBoolean(false)
    public BooleanR2kStruct animCyclic;
    @DM2FXOBinding("@anim_speed") @DM2LcfBinding(0x0C) @DM2LcfInteger(0)
    public IntegerR2kStruct animSpeed;

    public Tileset() {
        super("RPG::Tileset");
    }

    @Override
    protected IRIO dm2AddIVar(String sym) {
        if (sym.equals("@terrain_id_data"))
            return terrainTbl = new BlobR2kStruct("Table", new RubyTable(3, 162, 1, 1, new int[] {1}).innerBytes);
        if (sym.equals("@lowpass_data"))
            return lowPassTbl = new BlobR2kStruct("Table", bitfieldsToTable(R2kUtil.supplyBlank(162, (byte) 15).get()));
        if (sym.equals("@highpass_data")) {
            byte[] dat = new byte[144];
            for (int i = 0; i < dat.length; i++)
                dat[i] = 15;
            dat[0] = 31;
            return highPassTbl = new BlobR2kStruct("Table", bitfieldsToTable(dat));
        }
        return super.dm2AddIVar(sym);
    }

    @Override
    protected void dm2UnpackFromMapDestructively(HashMap<Integer, byte[]> pcd) {
        byte[] uv = pcd.get(3);
        if (uv != null) {
            // 162 = 144 (selective) + 18 (AT Field????)
            RubyTable rt = new RubyTable(3, 162, 1, 1, new int[] {0});
            // This relies on RubyTable layout to skip some relayout
            System.arraycopy(uv, 0, rt.innerBytes, 20, Math.min(uv.length, rt.innerBytes.length - 20));
            pcd.put(3, rt.innerBytes);
        }
        uv = pcd.get(4);
        if (uv != null)
            pcd.put(4, bitfieldsToTable(uv));
        uv = pcd.get(5);
        if (uv != null)
            pcd.put(5, bitfieldsToTable(uv));
        super.dm2UnpackFromMapDestructively(pcd);
    }

    @Override
    protected void dm2PackIntoMap(HashMap<Integer, byte[]> pcd) throws IOException {
        super.dm2PackIntoMap(pcd);
        byte[] uv = new byte[324];
        // This relies on RubyTable layout to skip some relayout
        System.arraycopy(terrainTbl.userVal, 20, uv, 0, Math.min(uv.length, terrainTbl.userVal.length - 20));
        pcd.put(3, uv);
        pcd.put(4, tableToBitfields(lowPassTbl.userVal));
        pcd.put(5, tableToBitfields(highPassTbl.userVal));
    }

    private byte[] bitfieldsToTable(byte[] dat) {
        RubyTable rt = new RubyTable(3, dat.length, 1, 1, new int[] {0});
        for (int i = 0; i < dat.length; i++)
            rt.innerBytes[20 + (i * 2)] = dat[i];
        return rt.innerBytes;
    }

    private byte[] tableToBitfields(byte[] src) {
        RubyTable rt = new RubyTable(src);
        byte[] r = new byte[rt.width];
        for (int i = 0; i < r.length; i++)
            r[i] = (byte) rt.getTiletype(i, 0, 0);
        return r;
    }
}
