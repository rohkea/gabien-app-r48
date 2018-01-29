/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.dbs;

import r48.RubyIO;

/**
 * Value syntax. Meant to be used from within EscapedStringSyntax or PathSyntax.
 * Most things are treated as int for compatibility.
 * However, " starts a string (no ending ")
 * and : starts a symbol.
 * Created on 10/06/17.
 */
public class ValueSyntax {
    public static RubyIO decode(String unescape) {
        if (unescape.startsWith("\"")) {
            return new RubyIO().setString(unescape.substring(2), true);
        } else if (unescape.startsWith(":")) {
            RubyIO sym = new RubyIO();
            sym.type = ':';
            sym.symVal = unescape.substring(1);
            return sym;
        } else {
            long i = Long.parseLong(unescape);
            return new RubyIO().setFX(i);
        }
    }

    // Can return null if unencodable.
    public static String encode(RubyIO val) {
        String v2 = "";
        if (val.type == '"') {
            v2 = "\"" + val.decString();
        } else if (val.type == ':') {
            v2 = ":" + val.symVal;
        } else if (val.type == 'i') {
            v2 += val.fixnumVal;
        }
        return v2;
    }
}