/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.schema.arrays;

import r48.RubyIO;
import r48.schema.OpaqueSchemaElement;
import r48.schema.SchemaElement;
import r48.schema.util.SchemaPath;

/**
 * Because sometimes you get curve balls thrown at you like "one-indexed arrays that aren't".
 * Created on 2/16/17.
 */
public class ArbIndexedArraySchemaElement extends ArraySchemaElement {
    public SchemaElement subelems;
    public int indexOffset;

    public ArbIndexedArraySchemaElement(SchemaElement s, int io, int atLeast, int fixedSize, IArrayInterface uiHelper) {
        super(fixedSize, (atLeast > -1) ? (atLeast + io) : 0, 0, uiHelper);
        subelems = s;
        indexOffset = io;
    }

    public ArbIndexedArraySchemaElement(SchemaElement s, int io, int atLeast, int fixedSize, IArrayInterface uiHelper, SchemaElement o) {
        super(fixedSize, (atLeast > -1) ? (atLeast + io) : 0, 0, uiHelper, o);
        subelems = s;
        indexOffset = io;
    }

    @Override
    protected boolean autoCorrectArray(RubyIO array, SchemaPath path) {
        return false;
    }

    @Override
    protected SchemaElement getElementSchema(int j) {
        // Opaque will always *default* to NIL if the result would otherwise be completely invalid data.
        if (j < indexOffset)
            return new OpaqueSchemaElement();
        return subelems;
    }

    @Override
    protected int elementPermissionsLevel(int i, RubyIO target) {
        if (i < indexOffset)
            return 0;
        return super.elementPermissionsLevel(i, target);
    }
}
