/*
 * gabien-app-r48 - Editing program for various formats
 * Written starting in 2016 by contributors (see CREDITS.txt)
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
 */

package r48.map.mapinfos;

import gabien.ui.IConsumer;
import r48.AppMain;
import r48.RubyIO;
import r48.io.data.IRIO;
import r48.schema.util.SchemaPath;
import r48.ui.Art;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Going to have to move it over here
 * Created on 02/06/17.
 */
public class RXPRMLikeMapInfoBackend implements IRMLikeMapInfoBackendWPub, IRMLikeMapInfoBackendWPriv {
    public IConsumer<SchemaPath> modHandler;
    public RubyIO mapInfos = AppMain.objectDB.getObject("MapInfos");

    public static String sNameFromInt(long key) {
        String mapStr = Long.toString(key);
        while (mapStr.length() < 3)
            mapStr = "0" + mapStr;
        return "Map" + mapStr;
    }

    @Override
    public void registerModificationHandler(IConsumer<SchemaPath> onMapInfoChange) {
        modHandler = onMapInfoChange;
        AppMain.objectDB.registerModificationHandler(mapInfos, onMapInfoChange);
    }

    @Override
    public Set<Long> getHashKeys() {
        HashSet<Long> hs = new HashSet<Long>();
        for (IRIO rio : mapInfos.hashVal.keySet())
            hs.add(rio.getFX());
        return hs;
    }

    @Override
    public RubyIO getHashBID(long k) {
        return mapInfos.getHashVal(new RubyIO().setFX(k));
    }

    @Override
    public int getOrderOfMap(long k) {
        return (int) mapInfos.getHashVal(new RubyIO().setFX(k)).getInstVarBySymbol("@order").fixnumVal;
    }

    @Override
    public long getMapOfOrder(int order) {
        for (Map.Entry<IRIO, RubyIO> rio : mapInfos.hashVal.entrySet())
            if (rio.getValue().getInstVarBySymbol("@order").fixnumVal == order)
                return rio.getKey().getFX();
        return -1;
    }

    @Override
    public boolean wouldRelocatingInOrderFail(int orderFrom, int orderTo) {
        return MapInfoReparentUtil.wouldRelocatingInOrderFail(orderFrom, orderTo, this);
    }

    @Override
    public int relocateInOrder(int orderFrom, int orderTo) {
        return MapInfoReparentUtil.relocateInOrder(orderFrom, orderTo, this);
    }

    @Override
    public void triggerEditInfoOf(long k) {
        AppMain.launchNonRootSchema(mapInfos, "File.MapInfos", new RubyIO().setFX(k), getHashBID(k), "RPG::MapInfo", "M" + k, null);
    }

    @Override
    public void swapOrders(int orderA, int orderB) {
        long a = getMapOfOrder(orderA);
        long b = getMapOfOrder(orderB);
        RubyIO ao = getHashBID(a).getInstVarBySymbol("@order");
        RubyIO bo = getHashBID(b).getInstVarBySymbol("@order");
        long t = bo.fixnumVal;
        bo.fixnumVal = ao.fixnumVal;
        ao.fixnumVal = t;
    }

    @Override
    public int getLastOrder() {
        int targetOrder = 0;
        for (long m : getHashKeys()) {
            int o = getOrderOfMap(m);
            if (o > targetOrder)
                targetOrder = o;
        }
        return targetOrder;
    }

    @Override
    public void removeMap(long k) {
        MapInfoReparentUtil.removeMapHelperSALT(k, this);
        mapInfos.removeHashVal(new RubyIO().setFX(k));
    }

    @Override
    public int createNewMap(long k) {
        RubyIO mi = mapInfos.addHashVal(new RubyIO().setFX(k));
        SchemaPath.setDefaultValue(mi, AppMain.schemas.getSDBEntry("RPG::MapInfo"), new RubyIO().setFX(k));
        int targetOrder = getLastOrder();
        long l = getMapOfOrder(targetOrder);
        if (l == -1)
            l = 0;
        mi.getInstVarBySymbol("@parent_id").fixnumVal = l;
        mi.getInstVarBySymbol("@order").fixnumVal = targetOrder + 1;
        return targetOrder;
    }

    @Override
    public void complete() {
        SchemaPath fakePath = new SchemaPath(AppMain.schemas.getSDBEntry("File.MapInfos"), mapInfos);
        AppMain.objectDB.objectRootModified(mapInfos, fakePath);
        modHandler.accept(fakePath);
    }

    @Override
    public Art.Symbol getIconForMap(long k) {
        return Art.Symbol.Map;
    }

    @Override
    public String translateToGUM(long k) {
        return sNameFromInt(k);
    }
}
