/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package r48.schema;

import gabien.ui.Rect;
import gabien.ui.UIElement;
import gabien.ui.UITextBox;
import r48.FontSizes;
import r48.schema.util.ISchemaHost;
import r48.RubyIO;
import r48.schema.util.SchemaPath;

import java.io.UnsupportedEncodingException;

/**
 * Created on 12/29/16.
 */
public class StringSchemaElement implements ISchemaElement {
    public String defaultStr = "";

    public StringSchemaElement(String arg) {
        defaultStr = arg;
    }

    @Override
    public UIElement buildHoldingEditor(final RubyIO target, final ISchemaHost launcher, final SchemaPath path) {
        final UITextBox tb = new UITextBox(FontSizes.schemaFieldTextHeight);
        tb.text = target.decString();
        tb.onEdit = new Runnable() {
            @Override
            public void run() {
                target.encString(tb.text);
                path.changeOccurred(false);
            }
        };
        tb.setBounds(new Rect(0, 0, 9, 9));
        return tb;
    }

    @Override
    public int maxHoldingHeight() {
        return UITextBox.getRecommendedSize(FontSizes.schemaFieldTextHeight).height;
    }

    @Override
    public void modifyVal(RubyIO target, SchemaPath path, boolean setDefault) {
        if (IntegerSchemaElement.ensureType(target, '\"', setDefault)) {
            try {
                target.strVal = defaultStr.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                target.strVal = new byte[0];
                e.printStackTrace();
            }
            path.changeOccurred(true);
        } else if (target.strVal == null) {
            try {
                target.strVal = defaultStr.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                target.strVal = new byte[0];
                e.printStackTrace();
            }
            path.changeOccurred(true);
        }
    }
}
