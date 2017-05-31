/*
 * This is released into the public domain.
 * No warranty is provided, implied or otherwise.
 */

package r48.dbs;

import gabien.ui.IFunction;
import r48.AppMain;
import r48.RubyIO;
import r48.schema.ISchemaElement;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created on 12/30/16.
 */
public class CMDB {
    public int digitCount = 3;
    public HashMap<Integer, RPGCommand> knownCommands = new HashMap<Integer, RPGCommand>();
    public LinkedList<Integer> knownCommandOrder = new LinkedList<Integer>();

    public CMDB(BufferedReader br) throws IOException {
        new DBLoader(br, new IDatabase() {
            RPGCommand rc;
            HashMap<String, ISchemaElement> localAliasing = new HashMap<String, ISchemaElement>();

            @Override
            public void newObj(int objId, String objName) {
                rc = new RPGCommand();
                rc.name = objName;
                knownCommands.put(objId, rc);
                knownCommandOrder.add(objId);
            }

            @Override
            public void execCmd(char c, String[] args) {
                if (c == 'p') {
                    rc.paramName.add(args[0].trim());
                    String s = args[1].trim();
                    final ISchemaElement se = aliasingAwareSG(s);
                    rc.paramType.add(new IFunction<RubyIO, ISchemaElement>() {
                        @Override
                        public ISchemaElement apply(RubyIO rubyIO) {
                            return se;
                        }
                    });
                }
                if (c == 'D') {
                    rc.paramName.add(args[0].trim());
                    final int arrayDI = Integer.parseInt(args[1]);
                    final ISchemaElement defaultSE = aliasingAwareSG(args[2]);
                    final HashMap<Integer, ISchemaElement> h = new HashMap<Integer, ISchemaElement>();
                    for (int i = 3; i < args.length; i += 2) {
                        int ind = Integer.parseInt(args[i]);
                        ISchemaElement se = aliasingAwareSG(args[i + 1]);
                        h.put(ind, se);
                    }
                    rc.paramType.add(new IFunction<RubyIO, ISchemaElement>() {
                        @Override
                        public ISchemaElement apply(RubyIO rubyIO) {
                            if (rubyIO == null)
                                return defaultSE;
                            if (rubyIO.arrVal == null)
                                return defaultSE;
                            if (rubyIO.arrVal.length <= arrayDI)
                                return defaultSE;
                            int p = (int) rubyIO.arrVal[arrayDI].fixnumVal;
                            ISchemaElement ise = h.get(p);
                            if (ise != null)
                                return ise;
                            return defaultSE;
                        }
                    });
                }
                if (c == 'i')
                    rc.indentPre = Integer.parseInt(args[0]);
                if (c == 'I')
                    rc.indentPost = Integer.parseInt(args[0]);
                if (c == 'K') {
                    rc.needsBlockLeavePre = true;
                    rc.blockLeaveReplacement = Integer.parseInt(args[0]);
                }
                if (c == 'l')
                    rc.needsBlockLeavePre = true;
                if (c == 'L')
                    rc.typeBlockLeave = true;
                if (c == '>')
                    localAliasing.put(args[0], AppMain.schemas.getSDBEntry(args[1]));
                if (c == 'X')
                    rc.specialSchemaName = args[0];
                if (c == 'C') {
                    if (args[0].equals("digitCount"))
                        digitCount = Integer.parseInt(args[1]);
                }
            }

            private ISchemaElement aliasingAwareSG(String s) {
                ISchemaElement se = localAliasing.get(s);
                if (se == null)
                    se = AppMain.schemas.getSDBEntry(s);
                return se;
            }
        });
    }

    public String buildCodename(RubyIO target, boolean indent) {
        String ext = "";
        int cid = (int) target.getInstVarBySymbol("@code").fixnumVal;
        if (knownCommands.containsKey(cid)) {
            RPGCommand cmd = knownCommands.get(cid);
            RubyIO params = target.getInstVarBySymbol("@parameters");
            ext = cmd.formatName(params, params.arrVal);
        }
        String spc = cid + " ";
        while (spc.length() < (digitCount + 1))
            spc = "0" + spc;
        RubyIO indentValue = target.getInstVarBySymbol("@indent");
        if ((indentValue != null) && indent) {
            int len = (int) target.getInstVarBySymbol("@indent").fixnumVal;
            if (len < 0) {
                spc += "(INDTERR" + len + ")";
            } else {
                for (int i = 0; i < len; i++)
                    spc += "_";
            }
        }
        return spc + ext;
    }
}
