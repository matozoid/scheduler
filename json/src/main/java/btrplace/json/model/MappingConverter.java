/*
 * Copyright (c) 2013 University of Nice Sophia-Antipolis
 *
 * This file is part of btrplace.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package btrplace.json.model;

import btrplace.json.AbstractJSONObjectConverter;
import btrplace.json.JSONConverterException;
import btrplace.model.DefaultMapping;
import btrplace.model.Mapping;
import net.minidev.json.JSONObject;

import java.util.UUID;

/**
 * Class to serialize and un-serialize {@link Mapping}.
 *
 * @author Fabien Hermenier
 */
public class MappingConverter extends AbstractJSONObjectConverter<Mapping> {

    @Override
    public JSONObject toJSON(Mapping c) {
        JSONObject o = new JSONObject();
        o.put("offlineNodes", uuidsToJSON(c.getOfflineNodes()));
        o.put("readyVMs", uuidsToJSON(c.getReadyVMs()));

        JSONObject ons = new JSONObject();
        for (UUID n : c.getOnlineNodes()) {
            JSONObject w = new JSONObject();
            w.put("runningVMs", uuidsToJSON(c.getRunningVMs(n)));
            w.put("sleepingVMs", uuidsToJSON(c.getSleepingVMs(n)));
            ons.put(n.toString(), w);
        }
        o.put("onlineNodes", ons);
        return o;
    }

    @Override
    public Mapping fromJSON(JSONObject o) throws JSONConverterException {
        Mapping c = new DefaultMapping();
        for (UUID u : requiredUUIDs(o, "offlineNodes")) {
            c.addOfflineNode(u);
        }
        for (UUID u : requiredUUIDs(o, "readyVMs")) {
            c.addReadyVM(u);
        }
        JSONObject ons = (JSONObject) o.get("onlineNodes");
        for (Object k : ons.keySet()) {
            UUID u = UUID.fromString((String) k);
            JSONObject on = (JSONObject) ons.get(k);
            c.addOnlineNode(u);
            for (UUID vmId : requiredUUIDs(on, "runningVMs")) {
                c.addRunningVM(vmId, u);
            }
            for (UUID vmId : requiredUUIDs(on, "sleepingVMs")) {
                c.addSleepingVM(vmId, u);
            }
        }

        return c;
    }
}
