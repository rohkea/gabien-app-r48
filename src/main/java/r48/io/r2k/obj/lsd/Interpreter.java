/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.io.r2k.obj.lsd;

import r48.io.data.DM2FXOBinding;
import r48.io.data.IRIO;
import r48.io.r2k.chunks.BooleanR2kStruct;
import r48.io.r2k.chunks.ByteR2kStruct;
import r48.io.r2k.chunks.IntegerR2kStruct;
import r48.io.r2k.dm2chk.*;
import r48.io.r2k.struct.EventCommand;

/**
 * aka 'SaveEventData'
 */
public class Interpreter extends DM2R2kObject {
    @DM2FXOBinding("@stack") @DM2LcfBinding(0x01) @DM2LcfSparseArrayA(InterpreterStackLevel.class)
    public DM2SparseArrayA<InterpreterStackLevel> commands;
    @DM2FXOBinding("@shown_message") @DM2LcfBinding(0x04) @DM2LcfBoolean(false)
    public BooleanR2kStruct shownMessage;
    @DM2FXOBinding("@keyii_wait") @DM2LcfBinding(0x15) @DM2LcfBoolean(false)
    public BooleanR2kStruct kiiWait;
    @DM2FXOBinding("@waiting_for_nmovement") @DM2LcfBinding(0x0D) @DM2LcfBoolean(false)
    public BooleanR2kStruct waitingMovementEnd;
    // Yes, really. Don't trust this
    @DM2FXOBinding("@keyii_variable_OLD") @DM2LcfBinding(0x16) @DM2LcfObject
    public ByteR2kStruct kiiVariable;
    @DM2FXOBinding("@keyii_timed") @DM2LcfBinding(0x29) @DM2LcfBoolean(false)
    public BooleanR2kStruct kiiTimed;
    @DM2FXOBinding("@keyii_time_variable") @DM2LcfBinding(0x20) @DM2LcfInteger(-1)
    public IntegerR2kStruct kiiTimeVariable;
    @DM2FXOBinding("@keyii_filter_arrowkeys") @DM2LcfBinding(0x17) @DM2LcfBoolean(false)
    public BooleanR2kStruct kiiFilterArrowkeys;
    @DM2FXOBinding("@keyii_filter_decision") @DM2LcfBinding(0x18) @DM2LcfBoolean(false)
    public BooleanR2kStruct kiiFilterDecision;
    @DM2FXOBinding("@waiting_for_sleep") @DM2LcfBinding(0x1F) @DM2LcfInteger(0)
    public IntegerR2kStruct waitingSlp1;
    @DM2FXOBinding("@keyii_filter_23") @DM2LcfBinding(0x23) @DM2LcfBoolean(false)
    public BooleanR2kStruct kiiFilter23;
    @DM2FXOBinding("@keyii_filter_24") @DM2LcfBinding(0x24) @DM2LcfBoolean(false)
    public BooleanR2kStruct kiiFilter24;
    @DM2FXOBinding("@keyii_filter_25") @DM2LcfBinding(0x25) @DM2LcfBoolean(false)
    public BooleanR2kStruct kiiFilter25;
    @DM2FXOBinding("@keyii_filter_26") @DM2LcfBinding(0x26) @DM2LcfBoolean(false)
    public BooleanR2kStruct kiiFilter26;
    @DM2FXOBinding("@keyii_filter_cancel") @DM2LcfBinding(0x19) @DM2LcfBoolean(false)
    public BooleanR2kStruct kiiFilterCancel;
    @DM2FXOBinding("@keyii_filter_1A") @DM2LcfBinding(0x1A) @DM2LcfBoolean(false)
    public BooleanR2kStruct kiiFilter1A;
    @DM2FXOBinding("@keyii_filter_1B") @DM2LcfBinding(0x1B) @DM2LcfBoolean(false)
    public BooleanR2kStruct kiiFilter1B;
    @DM2FXOBinding("@keyii_filter_1C") @DM2LcfBinding(0x1C) @DM2LcfBoolean(false)
    public BooleanR2kStruct kiiFilter1C;
    @DM2FXOBinding("@keyii_filter_1D") @DM2LcfBinding(0x1D) @DM2LcfBoolean(false)
    public BooleanR2kStruct kiiFilter1D;
    @DM2FXOBinding("@keyii_filter_1E") @DM2LcfBinding(0x1E) @DM2LcfBoolean(false)
    public BooleanR2kStruct kiiFilter1E;
    @DM2FXOBinding("@waiting_for_sleep_alt") @DM2LcfBinding(0x2A) @DM2LcfInteger(0)
    public IntegerR2kStruct waitingSlp2;

    public Interpreter() {
        super("RPG::Interpreter");
    }

    private class InterpreterStackLevel extends DM2R2kObject {
        @DM2FXOBinding("@list") @DM2LcfSizeBinding(0x1) @DM2LcfBinding(0x02) @DM2LcfBoolean(false)
        public DM2Array<EventCommand> list;
        @DM2FXOBinding("@index") @DM2LcfBinding(0x0B) @DM2LcfInteger(0)
        public IntegerR2kStruct index;
        @DM2FXOBinding("@event_id") @DM2LcfBinding(0x0C) @DM2LcfInteger(0)
        public IntegerR2kStruct eventId;
        @DM2FXOBinding("@actioned") @DM2LcfBinding(0x0D) @DM2LcfBoolean(false)
        public BooleanR2kStruct actioned;
        @DM2FXOBinding("@branches") @DM2LcfSizeBinding(0x15) @DM2LcfBinding(0x16)
        public DM2Array<ByteR2kStruct> branches;

        public InterpreterStackLevel() {
            super("RPG::InterpreterStackLevel");
        }

        @Override
        protected IRIO dm2AddIVar(String sym) {
            if (sym.equals("@list"))
                return list = new DM2Array<EventCommand>() {
                    @Override
                    public EventCommand newValue() {
                        return new EventCommand();
                    }
                };
            if (sym.equals("@branches"))
                return branches = new DM2Array<ByteR2kStruct>() {
                    @Override
                    public ByteR2kStruct newValue() {
                        return new ByteR2kStruct();
                    }
                };
            return super.dm2AddIVar(sym);
        }
    }
}
