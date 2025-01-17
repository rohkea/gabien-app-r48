/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.io.r2k.obj.ldb;

import r48.io.data.DM2FXOBinding;
import r48.io.data.IRIO;
import r48.io.r2k.chunks.BooleanR2kStruct;
import r48.io.r2k.chunks.IntegerR2kStruct;
import r48.io.r2k.chunks.StringR2kStruct;
import r48.io.r2k.dm2chk.*;
import r48.io.r2k.obj.Sound;

/**
 * Created on 05/06/17.
 */
public class Skill extends DM2R2kObject {
    @DM2FXOBinding("@name") @DM2LcfBinding(1) @DM2LcfObject
    public StringR2kStruct name;
    @DM2FXOBinding("@description") @DM2LcfBinding(2) @DM2LcfObject
    public StringR2kStruct description;
    @DM2FXOBinding("@use_text_1_2KO") @DM2LcfBinding(3) @DM2LcfObject
    public StringR2kStruct um1;
    @DM2FXOBinding("@use_text_2_2KO") @DM2LcfBinding(4) @DM2LcfObject
    public StringR2kStruct um2;
    @DM2FXOBinding("@failure_message_2KO") @DM2LcfBinding(7) @DM2LcfInteger(0)
    public IntegerR2kStruct fm;
    @DM2FXOBinding("@type") @DM2LcfBinding(8) @DM2LcfInteger(0)
    public IntegerR2kStruct type;
    @DM2FXOBinding("@sp_cost_percent_2k3") @DM2LcfBinding(9) @DM2LcfBoolean(false)
    public BooleanR2kStruct sp;
    @DM2FXOBinding("@sp_cost_val_percent_2k3") @DM2LcfBinding(10) @DM2LcfInteger(1)
    public IntegerR2kStruct spPercent;
    @DM2FXOBinding("@sp_cost_val_normal") @DM2LcfBinding(11) @DM2LcfInteger(0)
    public IntegerR2kStruct spCost;
    @DM2FXOBinding("@scope_n_healing") @DM2LcfBinding(12) @DM2LcfInteger(0)
    public IntegerR2kStruct scope;
    @DM2FXOBinding("@switch_control_target") @DM2LcfBinding(13) @DM2LcfInteger(1)
    public IntegerR2kStruct switchId;
    @DM2FXOBinding("@animation") @DM2LcfBinding(14) @DM2LcfInteger(0)
    public IntegerR2kStruct animationId;
    @DM2FXOBinding("@sound") @DM2LcfBinding(16) @DM2LcfObject
    public Sound soundEffect;
    @DM2FXOBinding("@usable_outside_battle") @DM2LcfBinding(18) @DM2LcfBoolean(true)
    public BooleanR2kStruct useOutBat;
    @DM2FXOBinding("@usable_in_battle") @DM2LcfBinding(19) @DM2LcfBoolean(false)
    public BooleanR2kStruct useInBat;
    @DM2FXOBinding("@add_states_2k3") @DM2LcfBinding(20) @DM2LcfBoolean(false)
    public BooleanR2kStruct stateAdd;
    @DM2FXOBinding("@phys_dmg_frac20") @DM2LcfBinding(21) @DM2LcfInteger(0)
    public IntegerR2kStruct physRate;
    @DM2FXOBinding("@mag_dmg_frac20") @DM2LcfBinding(22) @DM2LcfInteger(3)
    public IntegerR2kStruct magiRate;
    @DM2FXOBinding("@variance") @DM2LcfBinding(23) @DM2LcfInteger(4)
    public IntegerR2kStruct variRate;
    @DM2FXOBinding("@base_dmg") @DM2LcfBinding(24) @DM2LcfInteger(0)
    public IntegerR2kStruct power;
    @DM2FXOBinding("@hit_chance") @DM2LcfBinding(25) @DM2LcfInteger(100)
    public IntegerR2kStruct hit;
    @DM2FXOBinding("@mod_hp") @DM2LcfBinding(31) @DM2LcfBoolean(false)
    public BooleanR2kStruct affectHp;
    @DM2FXOBinding("@mod_sp") @DM2LcfBinding(32) @DM2LcfBoolean(false)
    public BooleanR2kStruct affectSp;
    @DM2FXOBinding("@mod_atk") @DM2LcfBinding(33) @DM2LcfBoolean(false)
    public BooleanR2kStruct affectAtk;
    @DM2FXOBinding("@mod_def") @DM2LcfBinding(34) @DM2LcfBoolean(false)
    public BooleanR2kStruct affectDef;
    @DM2FXOBinding("@mod_spi") @DM2LcfBinding(35) @DM2LcfBoolean(false)
    public BooleanR2kStruct affectSpi;
    @DM2FXOBinding("@mod_agi") @DM2LcfBinding(36) @DM2LcfBoolean(false)
    public BooleanR2kStruct affectAgi;
    @DM2FXOBinding("@steal_enemy_hp") @DM2LcfBinding(37) @DM2LcfBoolean(false)
    public BooleanR2kStruct absDam = new BooleanR2kStruct(false);
    @DM2FXOBinding("@ignore_def") @DM2LcfBinding(38) @DM2LcfBoolean(false)
    public BooleanR2kStruct igDef = new BooleanR2kStruct(false);
    @DM2FXOBinding("@mod_states") @DM2LcfSizeBinding(41) @DM2LcfBinding(42)
    public DM2ArraySet<BooleanR2kStruct> sEfx;
    @DM2FXOBinding("@mod_by_attributes") @DM2LcfSizeBinding(43) @DM2LcfBinding(44)
    public DM2ArraySet<BooleanR2kStruct> aEfx;
    @DM2FXOBinding("@affect_target_attr_defence") @DM2LcfBinding(45) @DM2LcfBoolean(false)
    public BooleanR2kStruct afAtDef;
    @DM2FXOBinding("@OFED_battler_anim_display_actor") @DM2LcfBinding(49) @DM2LcfInteger(1)
    public IntegerR2kStruct defBattlerAnim;
    @DM2FXOBinding("@battler_anim_data") @DM2LcfBinding(50) @DM2LcfSparseArray(BAD.class)
    public DM2SparseArrayH<BAD> battlerAnimMap;

    public Skill() {
        super("RPG::Skill");
    }

    @Override
    protected IRIO dm2AddIVar(String sym) {
        if (sym.equals("@mod_states"))
            return sEfx = boolSet();
        if (sym.equals("@mod_by_attributes"))
            return aEfx = boolSet();
        return super.dm2AddIVar(sym);
    }

    private DM2ArraySet<BooleanR2kStruct> boolSet() {
        return new DM2ArraySet<BooleanR2kStruct>() {
            @Override
            public BooleanR2kStruct newValue() {
                return new BooleanR2kStruct(false);
            }
        };
    }
}
