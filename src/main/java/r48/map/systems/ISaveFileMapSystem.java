/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */
package r48.map.systems;

import gabien.ui.IConsumer;
import gabien.ui.ISupplier;
import gabien.ui.UIElement;
import r48.IMapContext;

/**
 * Used to add an additional explorer.
 * Created on December 15th, 2017.
 */
public interface ISaveFileMapSystem {
    UIElement createSaveExplorer(final ISupplier<IConsumer<UIElement>> windowMaker, final IMapContext mapBox);
}
