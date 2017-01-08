/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabienapp.schema;

import gabien.ui.UIElement;
import gabien.ui.UILabel;
import gabienapp.schema.util.ISchemaHost;
import gabienapp.RubyIO;
import gabienapp.schema.util.SchemaPath;
import gabienapp.ui.UIHHalfsplit;

/**
 * NOTE: This doesn't provide the array entry object!!!
 *       This is because ArrayElementSchemaElement should only exist inside arrayDAM.
 * Created on 12/31/16.
 */
public class ArrayElementSchemaElement implements ISchemaElement {
    public int index;
    public String name;
    public ISchemaElement subSchema;
    public ArrayElementSchemaElement(int ind, String niceName, ISchemaElement ise) {
        index = ind;
        name = niceName;
        subSchema = ise;
    }
    @Override
    public UIElement buildHoldingEditor(RubyIO target, ISchemaHost launcher, SchemaPath path) {
        return new UIHHalfsplit(1, 3, new UILabel(name, false), subSchema.buildHoldingEditor(target.arrVal[index], launcher, path.arrayHashIndex(new RubyIO().setFX(index), "." + name)));
    }

    @Override
    public int maxHoldingHeight() {
        return Math.max(9, subSchema.maxHoldingHeight());
    }

    @Override
    public void modifyVal(RubyIO target, SchemaPath path, boolean setDefault) {
        // Resize array if required?
        if (target.arrVal.length <= index) {
            RubyIO[] newArr = new RubyIO[index + 1];
            for (int i = 0; i < newArr.length; i++)
                newArr[i] = new RubyIO().setNull();
            for (int i = 0; i < target.arrVal.length; i++)
                newArr[i] = target.arrVal[i];
            target.arrVal = newArr;
            path.changeOccurred(true);
        }
        subSchema.modifyVal(target.arrVal[index], path.arrayHashIndex(new RubyIO().setFX(index), "." + name), setDefault);
    }
}
