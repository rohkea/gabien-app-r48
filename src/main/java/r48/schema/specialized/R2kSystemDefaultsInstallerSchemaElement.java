/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.schema.specialized;

import gabien.ui.UIElement;
import gabien.ui.UISplitterLayout;
import gabien.ui.UITextButton;
import r48.AppMain;
import r48.FontSizes;
import r48.RubyIO;
import r48.RubyTable;
import r48.dbs.TXDB;
import r48.io.IObjectBackend;
import r48.io.data.IRIO;
import r48.map.events.R2kSavefileEventAccess;
import r48.map.mapinfos.R2kRMLikeMapInfoBackend;
import r48.schema.HiddenSchemaElement;
import r48.schema.SchemaElement;
import r48.schema.util.ISchemaHost;
import r48.schema.util.SchemaPath;

/**
 * Installs a set of sensible defaults on command.
 * NOTE: As of IRIOs this does have a slight bit of weirdness ; it assumes setArray for a few elements leaves the array empty, which may not always be the case.
 * Created on 08/06/17.
 */
public class R2kSystemDefaultsInstallerSchemaElement extends SchemaElement {
    public int mode = 0;

    public R2kSystemDefaultsInstallerSchemaElement(int i) {
        mode = i;
    }

    @Override
    public UIElement buildHoldingEditor(final IRIO target, ISchemaHost launcher, final SchemaPath path) {
        if (mode == 3) {
            UITextButton utb1 = new UITextButton(TXDB.get("Reset Events & Version (use after map change)"), FontSizes.schemaFieldTextHeight, new Runnable() {
                @Override
                public void run() {
                    // Before doing anything stupid...
                    long mapId = target.getIVar("@party_pos").getIVar("@map").getFX();
                    String mapName = R2kRMLikeMapInfoBackend.sNameFromInt((int) mapId);
                    IObjectBackend.ILoadedObject map = AppMain.objectDB.getObject(mapName, null);
                    if (map == null) {
                        AppMain.launchDialog(TXDB.get("The map's invalid, so that's not possible."));
                        return;
                    }
                    IRIO saveEvs = target.getIVar("@map_info").getIVar("@events");
                    saveEvs.setHash();
                    // Ghosts, become real!
                    IRIO hmr = map.getObject().getIVar("@events");
                    for (IRIO evs : hmr.getHashKeys())
                        R2kSavefileEventAccess.eventAsSaveEvent(saveEvs, mapId, evs, hmr.getHashVal(evs));
                    // @system save_count is in-game save count, not actual System @save_count
                    target.getIVar("@party_pos").getIVar("@map_save_count").setDeepClone(getSaveCount(map.getObject()));

                    IRIO ldbSys = AppMain.objectDB.getObject("RPG_RT.ldb").getObject().getIVar("@system");
                    IRIO saveCount = getSaveCount(ldbSys);

                    target.getIVar("@party_pos").getIVar("@db_save_count").setDeepClone(saveCount);
                    initTable(target.getIVar("@map_info").getIVar("@lower_tile_remap"));
                    initTable(target.getIVar("@map_info").getIVar("@upper_tile_remap"));

                    path.changeOccurred(false);
                    AppMain.launchDialog(TXDB.get("Reset events to map state and set versioning."));
                }
            });
            UITextButton utb2 = new UITextButton(TXDB.get("Try To Get RPG_RT To Reset The Map"), FontSizes.schemaFieldTextHeight, new Runnable() {
                @Override
                public void run() {
                    IRIO saveEvs = target.getIVar("@map_info").getIVar("@events");
                    saveEvs.setHash();
                    target.getIVar("@party_pos").getIVar("@map_save_count").setFX(0);
                    initTable(target.getIVar("@map_info").getIVar("@lower_tile_remap"));
                    initTable(target.getIVar("@map_info").getIVar("@upper_tile_remap"));

                    path.changeOccurred(false);
                    AppMain.launchDialog(TXDB.get("Ok, cleaned up. If RPG_RT loads this save, the map will probably be reset."));
                }
            });
            return new UISplitterLayout(utb1, utb2, true, 0.5d);
        } else {
            return HiddenSchemaElement.makeHiddenElement();
        }
    }

    public static IRIO getSaveCount(IRIO ldbSys) {
        IRIO saveCount = ldbSys.getIVar("@save_count_2k3en");
        if (saveCount == null)
            saveCount = ldbSys.getIVar("@save_count_other");
        if (saveCount == null)
            saveCount = new RubyIO().setFX(0);
        return saveCount;
    }

    @Override
    public void modifyVal(IRIO target, SchemaPath path, boolean setDefault) {
        if (setDefault) {
            // Target is RPG::Database.
            // Note that this relies on schema defaults for the most part,
            // it just puts some stuff that isn't so easily definable into place.
            // Tasks:
            IRIO sub;
            switch (mode) {
                case 0:
                    // 1. Install a basic Actor
                    SchemaPath.setDefaultValue(target.getIVar("@actors").addHashVal(new RubyIO().setFX(1)), AppMain.schemas.getSDBEntry("RPG::Actor"), new RubyIO().setFX(1));
                    target.getIVar("@system").getIVar("@party").setArray().addAElem(0).setFX(1);
                    // 2. Install a tileset
                    SchemaPath.setDefaultValue(target.getIVar("@tilesets").addHashVal(new RubyIO().setFX(1)), AppMain.schemas.getSDBEntry("RPG::Tileset"), new RubyIO().setFX(1));
                    // 3. Setup Terrain
                    SchemaPath.setDefaultValue(target.getIVar("@terrains").addHashVal(new RubyIO().setFX(1)), AppMain.schemas.getSDBEntry("RPG::Terrain"), new RubyIO().setFX(1));
                    // 4. Battle System initialization
                    sub = target.getIVar("@animations").addHashVal(new RubyIO().setFX(1));
                    SchemaPath.setDefaultValue(sub, AppMain.schemas.getSDBEntry("RPG::Animation"), new RubyIO().setFX(1));
                    sub.getIVar("@name").setString(TXDB.get("Default Fallback Animation"));

                    sub = target.getIVar("@states").addHashVal(new RubyIO().setFX(1));
                    SchemaPath.setDefaultValue(sub, AppMain.schemas.getSDBEntry("RPG::State"), new RubyIO().setFX(1));
                    // These are the minimum settings for death to work correctly.
                    sub.getIVar("@name").setString(TXDB.get("Death"));
                    sub.getIVar("@restriction").setFX(1);

                    sub = target.getIVar("@battle_anim_sets_2k3").addHashVal(new RubyIO().setFX(1));
                    SchemaPath.setDefaultValue(sub, AppMain.schemas.getSDBEntry("RPG::BattlerAnimationSet"), new RubyIO().setFX(1));
                    sub.getIVar("@name").setString(TXDB.get("Default Fallback AnimSet"));

                    // 5. Default enemy data
                    sub = target.getIVar("@enemies").addHashVal(new RubyIO().setFX(1));
                    SchemaPath.setDefaultValue(sub, AppMain.schemas.getSDBEntry("RPG::Enemy"), new RubyIO().setFX(1));

                    sub = target.getIVar("@troops").addHashVal(new RubyIO().setFX(1));
                    SchemaPath.setDefaultValue(sub, AppMain.schemas.getSDBEntry("RPG::Troop"), new RubyIO().setFX(1));
                    sub.getIVar("@name").setString(TXDB.get("Slime x1"));

                    sub = sub.getIVar("@members");
                    sub.addAElem(0).setNull();
                    SchemaPath.setDefaultValue(sub.addAElem(1), AppMain.schemas.getSDBEntry("RPG::Troop::Member"), new RubyIO().setFX(1));

                    // Prepare.
                    AppMain.pendingRunnables.add(new Runnable() {
                        @Override
                        public void run() {
                            AppMain.r2kProjectCreationHelperFunction();
                        }
                    });
                    break;
                case 1:
                    // 1. Fix root
                    sub = target.getIVar("@map_infos").addHashVal(new RubyIO().setFX(0));
                    SchemaPath.setDefaultValue(sub, AppMain.schemas.getSDBEntry("RPG::MapInfo"), new RubyIO().setFX(0));
                    sub.getIVar("@name").setString("Root");
                    sub.getIVar("@parent_id").setFX(0);
                    sub.getIVar("@indent").setFX(0);
                    sub.getIVar("@type").setFX(0);

                    // 2. Create basic map entry
                    sub = target.getIVar("@map_infos").addHashVal(new RubyIO().setFX(1));
                    SchemaPath.setDefaultValue(sub, AppMain.schemas.getSDBEntry("RPG::MapInfo"), new RubyIO().setFX(1));
                    sub.getIVar("@name").setString("First Map");
                    sub.getIVar("@parent_id").setFX(0);
                    sub.getIVar("@type").setFX(1);

                    // 3. Setup order
                    sub = target.getIVar("@map_order").setArray();
                    sub.addAElem(0).setFX(0);
                    sub.addAElem(1).setFX(1);
                    // 4. Setup start
                    target.getIVar("@start").getIVar("@player_map").setFX(1);
                    break;
                case 2:
                    // Nobody expects tilesets to act the way they do on defaults, FIX IT.
                    // I was informed to set upper to false by default, and though I have done that for most tiles,
                    //  my having to do this is a natural consequence.
                    RubyTable rt = new RubyTable(target.getIVar("@highpass_data").getBuffer());
                    rt.setTiletype(0, 0, 0, (short) 0x1F);
                    break;
                case 3:
                    // Savefile
                    saveFileSetup(target);
                    break;
                case 4:
                    // map_id saner default setter, but only for savefiles.
                    mapIdMagic(target, path.findRoot());
                    break;
            }
            // finally, signal
            path.changeOccurred(true);
        }
    }

    // sets target to the relevant map ID based on vague information
    private void mapIdMagic(IRIO target, SchemaPath root) {
        String str = AppMain.objectDB.getIdByObject(root.root);
        if (str == null)
            return;
        if (str.startsWith("Map"))
            if (str.endsWith(".lmu")) {
                try {
                    target.setFX(Integer.parseInt(str.substring(3, str.length() - 4)));
                } catch (Exception e) {
                    // nope
                }
            }
        if (str.startsWith("Save"))
            if (str.endsWith(".lsd"))
                target.setDeepClone(root.targetElement.getIVar("@party_pos").getIVar("@map"));
    }

    private void saveFileSetup(IRIO target) {
        setupSaveCharacter(target.getIVar("@party_pos"), "@player_map", "@player_x", "@player_y");
        setupSaveCharacter(target.getIVar("@boat_pos"), "@boat_map", "@boat_x", "@boat_y");
        setupSaveCharacter(target.getIVar("@ship_pos"), "@ship_map", "@ship_x", "@ship_y");
        setupSaveCharacter(target.getIVar("@airship_pos"), "@airship_map", "@airship_x", "@airship_y");
        // copy over stuff
        IRIO savSys = target.getIVar("@system");
        IRIO ldb = AppMain.objectDB.getObject("RPG_RT.ldb").getObject();
        IRIO ldbSys = ldb.getIVar("@system");

        // Copy over stuff that isn't optional (hmm. Should it be optional?)
        target.getIVar("@party").getIVar("@party").setDeepClone(ldbSys.getIVar("@party"));
        savSys.getIVar("@font_id").setDeepClone(ldbSys.getIVar("@font_id"));

        initializeArrayWithClones(savSys.getIVar("@switches"), ldb.getIVar("@switches"), new RubyIO().setBool(false));
        initializeArrayWithClones(savSys.getIVar("@variables"), ldb.getIVar("@variables"), new RubyIO().setFX(0));

        for (String iv : savSys.getIVars())
            if (iv.endsWith("_se") || iv.endsWith("_music") || iv.endsWith("_fadein") || iv.endsWith("_fadeout"))
                savSys.getIVar(iv).setDeepClone(ldbSys.getIVar(iv));

        // table init!
        initTable(target.getIVar("@map_info").getIVar("@lower_tile_remap"));
        initTable(target.getIVar("@map_info").getIVar("@upper_tile_remap"));
    }

    private void initTable(IRIO instVarBySymbol) {
        RubyTable rt = new RubyTable(instVarBySymbol.getBuffer());
        for (int i = 0; i < 0x90; i++)
            rt.setTiletype(i, 0, 0, (short) i);
    }

    private void initializeArrayWithClones(IRIO instVarBySymbol, IRIO length, IRIO rubyIO) {
        int maxVal = 0;
        for (IRIO rio : length.getHashKeys())
            maxVal = Math.max((int) rio.getFX(), maxVal);
        for (int i = 0; i < maxVal; i++)
            instVarBySymbol.addAElem(i).setDeepClone(rubyIO);
    }

    private void setupSaveCharacter(IRIO chr, String s, String s1, String s2) {
        IRIO lmt = AppMain.objectDB.getObject("RPG_RT.lmt").getObject().getIVar("@start");
        IRIO a = lmt.getIVar(s);
        IRIO b = lmt.getIVar(s1);
        IRIO c = lmt.getIVar(s2);
        if (a != null)
            chr.getIVar("@map").setDeepClone(a);
        if (b != null)
            chr.getIVar("@x").setDeepClone(b);
        if (c != null)
            chr.getIVar("@y").setDeepClone(c);
    }

    public static void upgradeDatabase(IRIO root) {
        // WARNING! This attempts to upgrade a project to 2003 from 2000 while causing as little damage as possible,
        //  but do consider that it might not actually *work*.
        IRIO system = root.getIVar("@system");
        system.getIVar("@ldb_id").setFX(2003);
        if (system.getIVar("@save_count_2k3en") == null)
            system.addIVar("@save_count_2k3en").setFX(0);
        if (system.getIVar("@menu_commands_2k3") == null) {
            IRIO mc23 = system.addIVar("@menu_commands_2k3");
            mc23.setArray();
            mc23.addAElem(0).setFX(5);
            mc23.addAElem(1).setFX(1);
            mc23.addAElem(2).setFX(2);
            mc23.addAElem(3).setFX(3);
            mc23.addAElem(4).setFX(4);
        }
    }
}
