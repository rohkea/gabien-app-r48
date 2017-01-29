/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package r48;

import gabien.ui.*;
import r48.ui.UIScrollVertLayout;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.Set;

/**
 * At first was a break-into-console - now a proper window, if crude.
 * Does the job better than the previous solution, in any case.
 * Got an update (12/31/16) to use UIScrollVertLayout...
 *  ...which is why it's now missing the useful left/right scroll control and the "DS" (save currently viewed object) button.
 * Created on 12/27/16.
 */
public class UITest extends UIPanel {
    public RubyIO currentObj;
    public String[] navigaList;
    public RubyIO[] objectList;
    int offset = 0;
    public LinkedList<RubyIO> back = new LinkedList<RubyIO>();
    public UIScrollVertLayout masterPanel = new UIScrollVertLayout();
    public UITest(RubyIO obj) {
        loadObject(obj);
        allElements.add(masterPanel);
        setBounds(new Rect(0, 0, 320, 200));
    }
    public void loadObject(final RubyIO obj) {
        offset = 0;
        currentObj = obj;
        LinkedList<String> strings = new LinkedList<String>();
        LinkedList<RubyIO> targs = new LinkedList<RubyIO>();
        // -- Actually collate things
        strings.add("Back from " + obj);
        targs.add(null);
        for (String s : sortedKeysStr(obj.iVars.keySet())) {
            strings.add("IVar " + s + " -> " + obj.iVars.get(s));
            targs.add(obj.iVars.get(s));
        }
        if (obj.hashVal != null) {
            for (RubyIO s : sortedKeys(obj.hashVal.keySet())) {
                strings.add(s + " -> " + obj.hashVal.get(s));
                targs.add(obj.hashVal.get(s));
            }
        }
        if (obj.arrVal != null) {
            for (int i = 0; i < obj.arrVal.length; i++) {
                RubyIO o = obj.arrVal[i];
                strings.add(i + " -> " + o);
                targs.add(o);
            }
        }
        // --
        navigaList = strings.toArray(new String[0]);
        objectList = targs.toArray(new RubyIO[0]);
        masterPanel.panels.clear();
        for (int i = 0; i < navigaList.length; i++) {
            final int j = i;
            UITextButton button = new UITextButton(i == 0, navigaList[i], new Runnable() {
                @Override
                public void run() {
                    if (objectList[j] == null) {
                        if (back.size() > 0)
                            loadObject(back.removeLast());
                    } else {
                        back.addLast(obj);
                        loadObject(objectList[j]);
                    }
                }
            });
            masterPanel.panels.add(button);
        }
    }

    @Override
    public void setBounds(Rect r) {
        super.setBounds(r);
        masterPanel.setBounds(new Rect(0, 0, r.width, r.height));
    }

    public static int natStrComp(String s, String s1) {
        // Notably, numeric length is major so numbered lists look right.
        // This is the reason a custom comparator was used.
        int nma = numLen(s);
        int nmb = numLen(s1);
        if (nma == 0)
            if (nmb == 0) {
                // Non-sortable via numerics, so stick to something sensible
                nma = s.length();
                nmb = s1.length();
            }

        if (nma < nmb)
            return -1;
        if (nma > nmb)
            return 1;

        // Ok, so natural length sorting didn't quite work out.
        while (true) {
            if (s.length() == 0)
                return 0;
            if (s1.length() == 0)
                return 0;
            char a = s.charAt(0);
            char b = s1.charAt(0);
            if (a > b)
                return 1;
            if (a < b)
                return -1;
            s = s.substring(1);
            s1 = s1.substring(1);
        }
    }

    private static int numLen(String s1) {
        int nm = 0;
        for (char c : s1.toCharArray()) {
            // Breaks at the first thing that couldn't conceivably be sortable.
            // (This usually gives a decently sorted list)
            if ((c >= '0') && (c <= '9')) {
                nm++;
            } else {
                break;
            }
        }
        return nm;
    }

    public static LinkedList<String> sortedKeysStr(Set<String> keys) {
        LinkedList<String> ios = new LinkedList<String>(keys);
        ios.sort(new Comparator<String>() {
            @Override
            public int compare(String t0, String t1) {
                return natStrComp(t0, t1);
            }
        });
        return ios;
    }

    public static LinkedList<RubyIO> sortedKeys(Set<RubyIO> rubyIOs) {
        LinkedList<RubyIO> ios = new LinkedList<RubyIO>(rubyIOs);
        ios.sort(new Comparator<RubyIO>() {
            @Override
            public int compare(RubyIO rubyIO, RubyIO t1) {
                return natStrComp(rubyIO.toString(), t1.toString());
            }
        });
        return ios;
    }
}