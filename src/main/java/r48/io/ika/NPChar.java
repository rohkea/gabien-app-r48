/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package r48.io.ika;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Handler for the NPChar file.
 * Like BM8I, adapted from earlier IkachanMapEdit code
 * Created on 1/27/17.
 */
public class NPChar {
    public NPCCharacter[] npcTable = new NPCCharacter[100];

    public NPChar() {
        for (int p = 0; p < npcTable.length; p++)
            npcTable[p] = new NPCCharacter();
    }

    // Apparently, negative values actually do work in this function!
    private static void doubleToFixedPoint(double d, FileOutputStream fos) throws IOException {
        int dd = (int) Math.floor(d);
        dd = dd << 14;
        double fractional = d - Math.floor(d);
        fractional *= 1 << 14;
        dd += (int) fractional;
        fos.write((dd & 0xFF));
        fos.write((dd & 0xFF00) >> 8);
        fos.write((dd & 0xFF0000) >> 16);
        fos.write((dd & 0xFF000000) >> 24);
    }

    private static double fixedPointToDouble(InputStream ins) throws IOException {
        int i = ins.read();
        i |= ins.read() << 8;
        i |= ins.read() << 16;
        i |= ins.read() << 24;
        return i / ((double) (1 << 14));
    }

    public void load(InputStream ins) throws IOException {
        for (int p = 0; p < npcTable.length; p++) {
            NPCCharacter nc = new NPCCharacter();
            boolean sw = ins.read() != 0;
            nc.collisionType = ins.read();
            nc.entityType = ins.read();
            nc.eventID = ins.read();
            nc.entityStatus = ins.read();// ?
            nc.entityStatus |= ins.read() << 8;
            nc.posX = fixedPointToDouble(ins);
            nc.posY = fixedPointToDouble(ins);
            nc.ofsX = fixedPointToDouble(ins);
            nc.ofsY = fixedPointToDouble(ins);
            nc.exists = sw;
            npcTable[p] = nc;
        }
    }

    public void save(FileOutputStream fos) throws IOException {
        for (int p = 0; p < npcTable.length; p++) {
            NPCCharacter nc = npcTable[p];
            if (nc.exists) {
                fos.write(1);
            } else {
                fos.write(0);
            }
            fos.write(nc.collisionType);
            fos.write(nc.entityType);
            fos.write(nc.eventID);
            fos.write(nc.entityStatus & 0xFF);
            fos.write((nc.entityStatus & 0xFF00) >> 8);
            doubleToFixedPoint(nc.posX, fos);
            doubleToFixedPoint(nc.posY, fos);
            doubleToFixedPoint(nc.ofsX, fos);
            doubleToFixedPoint(nc.ofsY, fos);
        }
    }

    public static class NPCCharacter {
        public boolean exists = false;
        public double posX, posY, ofsX, ofsY;
        public int entityType, collisionType, entityStatus, eventID;// collisiontype: 0:NULL 1:BLOCK 2:ENEMY 3:EVENT

        public NPCCharacter() {
        }
    }

}
