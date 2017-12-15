/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.map.mapinfos;

import gabien.ui.*;
import r48.AppMain;
import r48.FontSizes;
import r48.IMapContext;
import r48.RubyIO;
import r48.dbs.FormatSyntax;
import r48.dbs.TXDB;

/**
 * A 'flat' explorer showing just map information.
 */
public class UISaveScanMapInfos extends UIPanel {
    public final UIScrollLayout mainLayout = new UIScrollLayout(true, FontSizes.generalScrollersize);
    public final IFunction<Integer, String> objectMapping, gumMapping;
    public final IMapContext context;
    public final int first, last;

    public UISaveScanMapInfos(IFunction<Integer, String> map, IFunction<Integer, String> gummap, int f, int l, IMapContext ctx) {
        objectMapping = map;
        gumMapping = gummap;
        context = ctx;
        first = f;
        last = l;
        allElements.add(mainLayout);
        reload();
    }

    @Override
    public void setBounds(Rect r) {
        super.setBounds(r);
        mainLayout.setBounds(new Rect(0, 0, r.width, r.height));
    }

    public void reload() {
        for (int i = first; i <= last; i++) {
            RubyIO rio = AppMain.objectDB.getObject(objectMapping.apply(i), null);
            if (rio != null) {
                final String gum = gumMapping.apply(i);
                mainLayout.panels.add(new UITextButton(FontSizes.mapInfosTextHeight, FormatSyntax.formatExtended(TXDB.get("#A : #B"), new RubyIO().setInternString(gum), rio), new Runnable() {
                    @Override
                    public void run() {
                        context.loadMap(gum);
                    }
                }));
            }
        }
    }
}