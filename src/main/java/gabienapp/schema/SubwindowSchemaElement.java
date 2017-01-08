/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package gabienapp.schema;

import gabien.ui.IFunction;
import gabien.ui.UIElement;
import gabien.ui.UITextButton;
import gabienapp.schema.util.ISchemaHost;
import gabienapp.RubyIO;
import gabienapp.schema.util.SchemaPath;

/**
 * Created on 12/29/16.
 */
public class SubwindowSchemaElement implements ISchemaElement {
    public ISchemaElement heldElement;
    public IFunction<RubyIO, String> nameGetter = new IFunction<RubyIO, String>() {
        @Override
        public String apply(RubyIO rubyIO) {
            return rubyIO.toString();
        }
    };

    public SubwindowSchemaElement(ISchemaElement encap) {
        heldElement = encap;

    }

    public SubwindowSchemaElement(ISchemaElement encap, IFunction<RubyIO, String> naming) {
        heldElement = encap;
        nameGetter = naming;
    }

    @Override
    public UIElement buildHoldingEditor(final RubyIO target, final ISchemaHost launcher, final SchemaPath path) {
        return new UITextButton(false, nameGetter.apply(target), new Runnable() {
            @Override
            public void run() {
                launcher.switchObject(path.newWindow(heldElement, target, launcher));
            }
        });
    }

    @Override
    public int maxHoldingHeight() {
        return 10;
    }

    @Override
    public void modifyVal(RubyIO target, SchemaPath path, boolean setDefault) {
        heldElement.modifyVal(target, path, setDefault);
    }
}
