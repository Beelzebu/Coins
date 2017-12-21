/**
 * This file is part of Coins.
 *
 * Copyright © 2018 Beelzebu
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.nifheim.beelzebu.coins.core.multiplier;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @author Beelzebu
 */
public final class MultiplierBuilder {
    
    private String server = "";
    private MultiplierType type = MultiplierType.SERVER;
    private MultiplierData data = new MultiplierData(-1, -1);
    private int id = -1;
    private String enablerName = null;
    private UUID enablerUUID = null;
    private Set<MultiplierData> extradata = Collections.emptySet();
    private boolean enabled = false;
    private boolean queue =  false;

    private MultiplierBuilder() {
    }

    public static MultiplierBuilder newBuilder() {
        return new MultiplierBuilder();
    }
    
    public MultiplierBuilder setServer(String server) {
        this.server = server;
        return this;
    }
    
    public MultiplierBuilder setType(MultiplierType type) {
        this.type = type;
        return this;
    }
    
    public MultiplierBuilder setData(MultiplierData data) {
        this.data = data;
        return this;
    }
    
    public MultiplierBuilder setID(int id) {
        this.id = id;
        return this;
    }
    
    public MultiplierBuilder setEnablerName(String enablerName) {
        this.enablerName = enablerName;
        return this;
    }
    
    public MultiplierBuilder setEnablerUUID(UUID uuid) {
        this.enablerUUID = uuid;
        return this;
    }
    
    public MultiplierBuilder setExtraData(Set<MultiplierData> extradata) {
        this.extradata = extradata;
        return this;
    }
    
    public MultiplierBuilder setAmount(int amount) {
        data.setAmount(amount);
        return this;
    }
    
    public MultiplierBuilder setMinutes(int minutes) {
        data.setMinutes(minutes);
        return this;
    }
    
    public MultiplierBuilder setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }
    
    public MultiplierBuilder setQueue(boolean queue) {
        this.queue = queue;
        return this;
    }
    
    public Multiplier build() {
        Multiplier multiplier = new Multiplier(server, data);
        multiplier.setType(type);
        multiplier.setId(id);
        multiplier.setEnablerName(enablerName);
        multiplier.setEnablerUUID(enablerUUID);
        multiplier.setExtradata(extradata);
        multiplier.setQueue(queue);
        if (enabled) {
            multiplier.enable(enablerUUID, enablerName, queue);
        }
        return multiplier;
    }
}
