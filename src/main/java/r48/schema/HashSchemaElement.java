/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.schema;

import gabien.ui.*;
import r48.FontSizes;
import r48.RubyIO;
import r48.UITest;
import r48.dbs.IProxySchemaElement;
import r48.dbs.TXDB;
import r48.io.data.IRIO;
import r48.schema.specialized.OSStrHashMapSchemaElement;
import r48.schema.util.ISchemaHost;
import r48.schema.util.SchemaPath;
import r48.ui.UIAppendButton;

import java.util.HashMap;
import java.util.Map;

/**
 * Created on 12/29/16.
 */
public class HashSchemaElement extends SchemaElement {
    public SchemaElement keyElem, valElem;
    public boolean flexible;

    public HashSchemaElement(SchemaElement keySE, SchemaElement opaqueSE, boolean flexible) {
        keyElem = keySE;
        valElem = opaqueSE;
        this.flexible = flexible;
    }

    @Override
    public UIElement buildHoldingEditor(final RubyIO target, final ISchemaHost launcher, final SchemaPath path) {
        final UIScrollLayout uiSV = AggregateSchemaElement.createScrollSavingSVL(launcher, this, target);
        RubyIO preWorkspace = (RubyIO) launcher.getEmbedObject(this, target, "keyWorkspace");
        if (preWorkspace == null) {
            preWorkspace = new RubyIO().setNull();
            SchemaPath.setDefaultValue(preWorkspace, keyElem, null);
        } else {
            preWorkspace = new RubyIO().setDeepClone(preWorkspace);
        }
        final RubyIO keyWorkspace = preWorkspace;

        final SchemaPath rioPath = new SchemaPath(keyElem, keyWorkspace);

        final SchemaPath setLocalePath = launcher.getCurrentObject();
        rioPath.additionalModificationCallback = new Runnable() {
            @Override
            public void run() {
                // This may occur from a different page (say, an enum selector), so the more complicated form must be used.
                launcher.setEmbedObject(setLocalePath, HashSchemaElement.this, target, "keyWorkspace", new RubyIO().setDeepClone(keyWorkspace));
            }
        };
        if (keyWorkspace.type == 'i') {
            while (target.getHashVal(keyWorkspace) != null) {
                // Try adding 1
                long plannedVal = ++keyWorkspace.fixnumVal;
                keyElem.modifyVal(keyWorkspace, rioPath, false);
                if ((keyWorkspace.type != 'i') || (keyWorkspace.fixnumVal != plannedVal)) {
                    // Let's not try that again
                    break;
                }
            }
        }
        // similar to the array schema, this is a containing object with access to local information
        Runnable rebuildSection = new Runnable() {
            // "Here come the hax!"
            // Also does relayout
            public void trigger() {
                run();
            }
            @Override
            public void run() {
                uiSV.panelsClear();
                final UITextBox searchBox = new UITextBox("", FontSizes.schemaFieldTextHeight);
                String oldSearchTerm = (String) launcher.getEmbedObject(HashSchemaElement.this, target, "searchTerm");
                if (oldSearchTerm != null)
                    searchBox.text = oldSearchTerm;
                searchBox.onEdit = new Runnable() {
                    @Override
                    public void run() {
                        launcher.setEmbedObject(HashSchemaElement.this, target, "searchTerm", searchBox.text);
                        trigger();
                    }
                };
                uiSV.panelsAdd(new UISplitterLayout(new UILabel(TXDB.get("Search Keys:"), FontSizes.schemaFieldTextHeight), searchBox, false, 0d));
                for (IRIO key : UITest.sortedKeys(target.hashVal.keySet(), new IFunction<IRIO, String>() {
                    @Override
                    public String apply(IRIO rubyIO) {
                        return getKeyText(rubyIO);
                    }
                })) {
                    if (!getKeyText(key).contains(searchBox.text))
                        continue;
                    final IRIO kss = key;
                    // keys are opaque - this prevents MANY issues
                    UIElement hsA = (new OpaqueSchemaElement() {
                        @Override
                        public String getMessage(IRIO v) {
                            return getKeyText(v);
                        }
                    }).buildHoldingEditor(key, launcher, path);
                    UIElement hsB = valElem.buildHoldingEditor(target.hashVal.get(key), launcher, path.arrayHashIndex(key, "{" + getKeyText(key) + "}"));
                    UISplitterLayout hs = null;
                    if (flexible) {
                        hs = new UISplitterLayout(hsA, hsB, true, 0.0d);
                    } else {
                        hs = new UISplitterLayout(hsA, hsB, false, 0.5d);
                    }
                    uiSV.panelsAdd(new UIAppendButton("-", hs, new Runnable() {
                        @Override
                        public void run() {
                            // remove
                            target.hashVal.remove(kss);
                            path.changeOccurred(false);
                            // auto-updates
                        }
                    }, FontSizes.schemaFieldTextHeight));
                }
                // Set up a key workspace.
                UIElement workspace = keyElem.buildHoldingEditor(keyWorkspace, launcher, rioPath);
                UISplitterLayout workspaceHS = new UISplitterLayout(workspace, new UITextButton(TXDB.get("Add Key"), FontSizes.schemaFieldTextHeight, new Runnable() {
                    @Override
                    public void run() {
                        if (target.getHashVal(keyWorkspace) == null) {
                            RubyIO rio2 = new RubyIO();
                            RubyIO finWorkspace = new RubyIO().setDeepClone(keyWorkspace);
                            valElem.modifyVal(rio2, path.arrayHashIndex(finWorkspace, "{" + getKeyText(finWorkspace) + "}"), true);
                            target.hashVal.put(finWorkspace, rio2);
                            // the deep clone prevents further modification of the key
                            path.changeOccurred(false);
                            // auto-updates
                        }
                    }
                }), false, 2, 3);
                uiSV.panelsAdd(workspaceHS);
            }
        };
        rebuildSection.run();
        return uiSV;
    }

    private String getKeyText(IRIO v) {
        SchemaElement ke = keyElem;
        while (ke instanceof IProxySchemaElement)
            ke = ((IProxySchemaElement) ke).getEntry();
        if (ke instanceof EnumSchemaElement)
            return ((EnumSchemaElement) ke).viewValue(v, true);
        if (ke instanceof OSStrHashMapSchemaElement)
            return OSStrHashMapSchemaElement.decode(v);
        return TXDB.get("Key " + v);
    }

    @Override
    public void modifyVal(RubyIO target, SchemaPath path, boolean setDefault) {
        setDefault = SchemaElement.ensureType(target, '{', setDefault);
        if (setDefault) {
            target.hashVal = new HashMap<IRIO, RubyIO>();
            path.changeOccurred(true);
        } else {
            if (target.hashVal == null) {
                target.hashVal = new HashMap<IRIO, RubyIO>();
                path.changeOccurred(true);
            }
            for (Map.Entry<IRIO, RubyIO> e : target.hashVal.entrySet())
                valElem.modifyVal(e.getValue(), path.arrayHashIndex(e.getKey(), "{" + getKeyText(e.getKey()) + "}"), false);
        }
    }
}
