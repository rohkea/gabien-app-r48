/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package r48.toolsets;

import gabien.ui.IConsumer;
import gabien.ui.ISupplier;
import gabien.ui.UIElement;

/**
 * Hopefully will allow cleaning up the initial tab creation code.
 * Created on 2/12/17.
 */
public interface IToolset {
    String[] tabNames();

    // NOTE: This allows skipping out on actually generating tabs at the end, if you dare.
    UIElement[] generateTabs(ISupplier<IConsumer<UIElement>> windowMaker);
}
