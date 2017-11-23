/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.io;

import gabien.GaBIEn;
import gabienapp.Application;
import r48.RubyIO;
import r48.io.r2k.files.DatabaseIO;
import r48.io.r2k.files.MapIO;
import r48.io.r2k.files.MapTreeIO;
import r48.io.r2k.files.SaveDataIO;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A beginning?
 * Created on 30/05/17.
 */
public class R2kObjectBackend implements IObjectBackend {
    public final String root;

    public R2kObjectBackend(String rootPath) {
        root = rootPath;
    }

    @Override
    public RubyIO loadObjectFromFile(String filename) {
        filename = root + filename;
        String str = Application.autoDetectWindows(filename);
        if (filename.endsWith(".lmu")) {
            try {
                InputStream fis = GaBIEn.getFile(str);
                if (fis == null)
                    return null;
                RubyIO r = MapIO.readLmu(fis);
                fis.close();
                return r;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        if (filename.endsWith(".lmt")) {
            try {
                InputStream fis = GaBIEn.getFile(str);
                if (fis == null)
                    return null;
                RubyIO r = MapTreeIO.readLmt(fis);
                fis.close();
                return r;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        if (filename.endsWith(".ldb")) {
            try {
                InputStream fis = GaBIEn.getFile(str);
                if (fis == null)
                    return null;
                RubyIO r = DatabaseIO.readLdb(fis);
                fis.close();
                return r;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        if (filename.endsWith(".lsd")) {
            try {
                InputStream fis = GaBIEn.getFile(str);
                if (fis == null)
                    return null;
                RubyIO r = SaveDataIO.readLsd(fis);
                fis.close();
                return r;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    @Override
    public void saveObjectToFile(String filename, RubyIO object) throws IOException {
        filename = root + filename;
        String str = Application.autoDetectWindows(filename);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Note the write occurs before the F.O.S is created for safety
        if (filename.endsWith(".lmu")) {
            MapIO.writeLmu(baos, object);
            OutputStream fos = GaBIEn.getOutFile(str);
            if (fos == null)
                throw new IOException("Unable to open a file.");
            baos.writeTo(fos);
            fos.close();
            return;
        }
        if (filename.endsWith(".lmt")) {
            MapTreeIO.writeLmt(baos, object);
            OutputStream fos = GaBIEn.getOutFile(str);
            if (fos == null)
                throw new IOException("Unable to open a file.");
            baos.writeTo(fos);
            fos.close();
            return;
        }
        if (filename.endsWith(".ldb")) {
            DatabaseIO.writeLdb(baos, object);
            OutputStream fos = GaBIEn.getOutFile(str);
            if (fos == null)
                throw new IOException("Unable to open a file.");
            baos.writeTo(fos);
            fos.close();
            return;
        }
        if (filename.endsWith(".lsd")) {
            SaveDataIO.writeLsd(baos, object);
            OutputStream fos = GaBIEn.getOutFile(str);
            if (fos == null)
                throw new IOException("Unable to open a file.");
            baos.writeTo(fos);
            fos.close();
            return;
        }
        throw new IOException("Unknown how to save " + filename + " (lmu/lmt/ldb)");
    }
}
