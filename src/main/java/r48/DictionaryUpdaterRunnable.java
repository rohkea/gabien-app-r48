/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package r48;

import gabien.ui.IFunction;
import r48.schema.EnumSchemaElement;
import r48.schema.ISchemaElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to build convenient dictionaries for selecting things.
 * Created on 1/3/17.
 */
public class DictionaryUpdaterRunnable implements Runnable {
    // act soon after init.
    private boolean actNow = true;
    public final String dict, targ, iVar;
    // Responsible for removing any initial wrapping
    public final IFunction<RubyIO, RubyIO> fieldA;
    public final boolean hash;
    public DictionaryUpdaterRunnable(String targetDictionary, String target, IFunction<RubyIO, RubyIO> iFunction, boolean b, String ivar) {
        dict = targetDictionary;
        targ = target;
        fieldA = iFunction;
        hash = b;
        iVar = ivar;
        AppMain.schemas.getSDBEntry(targetDictionary);
    }

    public void actIfRequired() {
        if (actNow) {
            actNow = false;
            // actually update
            HashMap<String, Integer> finalMap = new HashMap<String, Integer>();
            RubyIO target = AppMain.objectDB.getObject(targ);
            if (fieldA != null)
                target = fieldA.apply(target);
            if (hash) {
                for (Map.Entry<RubyIO, RubyIO> rio : target.hashVal.entrySet()) {
                    handleVal(finalMap, rio.getValue(), (int) rio.getKey().fixnumVal);
                }
            } else {
                for (int i = 0; i < target.arrVal.length; i++) {
                    RubyIO rio = target.arrVal[i];
                    handleVal(finalMap, rio, i);
                }
            }
            ISchemaElement ise = new EnumSchemaElement(finalMap, "ID.");
            AppMain.schemas.setSDBEntry(dict, ise);
        }
    }

    private void handleVal(HashMap<String, Integer> finalMap, RubyIO rio, int fixnumVal) {
        if (rio.type != '0') {
            if (iVar == null) {
                finalMap.put(fixnumVal + ":" + rio.decString(), fixnumVal);
            } else {
                finalMap.put(fixnumVal + ":" + rio.getInstVarBySymbol(iVar).decString(), fixnumVal);
            }
        }
    }

    @Override
    public void run() {
        actNow = true;
    }
}