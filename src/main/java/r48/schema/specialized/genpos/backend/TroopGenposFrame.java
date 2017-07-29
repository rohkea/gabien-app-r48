/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package r48.schema.specialized.genpos.backend;

import gabien.IGrInDriver;
import gabien.ui.UILabel;
import r48.AppMain;
import r48.ArrayUtils;
import r48.RubyIO;
import r48.RubyTable;
import r48.dbs.TXDB;
import r48.map.imaging.IImageLoader;
import r48.schema.BooleanSchemaElement;
import r48.schema.SchemaElement;
import r48.schema.integers.IntBooleanSchemaElement;
import r48.schema.integers.IntegerSchemaElement;
import r48.schema.specialized.genpos.IGenposFrame;
import r48.schema.util.SchemaPath;

import java.util.Map;

/**
 * Created on 28/07/17.
 */
public class TroopGenposFrame implements IGenposFrame {

    public static final int[] gameBattleDisplay = new int[] {
                // 320x160 (RPG Maker 2000)
                0, 0,
                320, 0,
                0, 160,
                320, 160,
                // 2k3?
                0, 240,
                320, 240
    };

    public IGrInDriver.IImage battleBkg;
    public RubyIO troop;
    public SchemaPath troopPath;
    public IGrInDriver.IImage[] enemies;
    public Runnable changed;
    public TroopGenposFrame(RubyIO t, SchemaPath path, Runnable change) {
        troop = t;
        troopPath = path;
        changed = change;
        // Immediately try and get needed resources
        RubyIO database = AppMain.objectDB.getObject("RPG_RT.ldb");
        IImageLoader img = AppMain.stuffRendererIndependent.imageLoader;
        battleBkg = img.getImage("Backdrop/" + database.getInstVarBySymbol("@system").getInstVarBySymbol("@test_battle_background").decString(), true);
        long max = 0;
        for (RubyIO rio : database.getInstVarBySymbol("@enemies").hashVal.keySet())
            max = Math.max(max, rio.fixnumVal);
        enemies = new IGrInDriver.IImage[(int) (max + 1)];
        for (Map.Entry<RubyIO, RubyIO> map : database.getInstVarBySymbol("@enemies").hashVal.entrySet())
            enemies[(int) (map.getKey().fixnumVal)] = readEnemy(map.getValue(), img);
    }

    private IGrInDriver.IImage readEnemy(RubyIO value, IImageLoader img) {
        return img.getImage("Monster/" + value.getInstVarBySymbol("@battler_name").decString(), false);
    }

    @Override
    public int[] getIndicators() {
        return gameBattleDisplay;
    }

    @Override
    public boolean canAddRemoveCells() {
        return true;
    }

    @Override
    public void addCell(int i2) {
        RubyIO rio = SchemaPath.createDefaultValue(AppMain.schemas.getSDBEntry("RPG::Troop::Member"), new RubyIO().setFX(i2));
        ArrayUtils.insertRioElement(troop.getInstVarBySymbol("@members"), rio, i2);
        changed.run();
    }

    @Override
    public void deleteCell(int i2) {
        ArrayUtils.removeRioElement(troop.getInstVarBySymbol("@members"), i2);
        changed.run();
    }

    private SchemaElement[] getCellPropSchemas() {
        return new SchemaElement[] {
                AppMain.schemas.getSDBEntry("enemy_id"),
                new IntegerSchemaElement(0),
                new IntegerSchemaElement(0),
                new BooleanSchemaElement(false)
        };
    }

    @Override
    public SchemaPath getCellProp(int ct, int i) {
        SchemaPath memberPath = troopPath.otherIndex("@members").arrayHashIndex(new RubyIO().setFX(ct + 1), "[" + (ct + 1) + "]");
        RubyIO member = troop.getInstVarBySymbol("@members").arrVal[ct + 1];
        SchemaElement se = getCellPropSchemas()[i];
        if (i == 0)
            return memberPath.newWindow(se, member.getInstVarBySymbol("@enemy"), null);
        if (i == 1)
            return memberPath.newWindow(se, member.getInstVarBySymbol("@x"), null);
        if (i == 2)
            return memberPath.newWindow(se, member.getInstVarBySymbol("@y"), null);
        if (i == 3)
            return memberPath.newWindow(se, member.getInstVarBySymbol("@invisible"), null);
        throw new RuntimeException("Invalid cell prop.");
    }

    @Override
    public void moveCell(int ct, int x, int y) {
        SchemaPath memberPath = troopPath.otherIndex("@members").arrayHashIndex(new RubyIO().setFX(ct + 1), "[" + (ct + 1) + "]");
        RubyIO member = troop.getInstVarBySymbol("@members").arrVal[ct + 1];
        member.getInstVarBySymbol("@x").fixnumVal += x;
        member.getInstVarBySymbol("@y").fixnumVal += y;
        memberPath.changeOccurred(false);
    }

    @Override
    public int getCellCount() {
        return Math.max(0, troop.getInstVarBySymbol("@members").arrVal.length - 1);
    }

    @Override
    public String[] getCellProps() {
        return new String[] {
                TXDB.get("enemyId"),
                TXDB.get("x"),
                TXDB.get("y"),
                TXDB.get("invisible")
        };
    }

    @Override
    public void drawCellSelectionIndicator(int i, int opx, int opy, IGrInDriver igd) {
        int x = (int) getCellProp(i, 1).targetElement.fixnumVal;
        int y = (int) getCellProp(i, 2).targetElement.fixnumVal;
        igd.blitImage(36, 0, 32, 32, opx + x - 16, opy + y - 16, AppMain.layerTabs);
    }

    @Override
    public void drawCell(int i, int opx, int opy, IGrInDriver igd) {
        // hm.
        int enemy = (int) getCellProp(i, 0).targetElement.fixnumVal;
        opx += getCellProp(i, 1).targetElement.fixnumVal;
        opy += getCellProp(i, 2).targetElement.fixnumVal;
        if (enemy < 0)
            return;
        if (enemy >= enemies.length)
            return;
        IGrInDriver.IImage enemyImg = enemies[enemy];
        if (enemyImg != null)
            igd.blitImage(0, 0, enemyImg.getWidth(), enemyImg.getHeight(), opx - (enemyImg.getWidth() / 2), opy - (enemyImg.getHeight() / 2), enemyImg);
    }

    @Override
    public IGrInDriver.IImage getBackground() {
        return battleBkg;
    }
}