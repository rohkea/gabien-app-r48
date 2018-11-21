/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.io.r2k.chunks;

import r48.RubyIO;
import r48.io.data.IRIO;

/**
 * My goodness, the format is madness.
 * But I must continue.
 * R2kObject is a subclass of this for Lcf Chunked objects.
 * Anyway, do note that some of these might be "single-use".
 * What this means is that they have a strict lifecycle of create, import, convert, and then die.
 * These elements will error if used incorrectly.
 * Created on 31/05/17.
 */
public interface IR2kStruct extends IR2kInterpretable {
    RubyIO asRIO();

    void fromRIO(IRIO src);
}
