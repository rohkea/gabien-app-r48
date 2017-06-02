/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */
package r48.io.r2k.obj;

import gabien.ui.ISupplier;
import r48.RubyIO;
import r48.io.r2k.Index;
import r48.io.r2k.R2kUtil;
import r48.io.r2k.chunks.*;

/**
 * Created on 31/05/17.
 */
public class Event extends R2kObject {
    public StringR2kStruct name = new StringR2kStruct();
    public IntegerR2kStruct x = new IntegerR2kStruct(0);
    public IntegerR2kStruct y = new IntegerR2kStruct(0);
    public SparseArrayAR2kStruct<EventPage> pages = new SparseArrayAR2kStruct<EventPage>(new ISupplier<EventPage>() {
        @Override
        public EventPage get() {
            return new EventPage();
        }
    });

    public Index[] getIndices() {
        return new Index[] {
                new Index(0x01, name, "@name"),
                new Index(0x02, x, "@x"),
                new Index(0x03, y, "@y"),
                new Index(0x05, pages, "@pages")
        };
    }

    @Override
    public RubyIO asRIO() {
        RubyIO mt = new RubyIO().setSymlike("RPG::Event", true);
        asRIOISF(mt);
        return mt;
    }

    @Override
    public void fromRIO(RubyIO src) {
        fromRIOISF(src);
    }
}