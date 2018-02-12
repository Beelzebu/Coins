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

import com.google.common.collect.Sets;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang.Validate;

/**
 *
 * @author Beelzebu
 */
public final class MultiplierBuilder {

    private String server = "";
    private MultiplierType type = MultiplierType.SERVER;
    private MultiplierData data = new MultiplierData(UUID.randomUUID(), "", -1, -1);
    private int id = -1;
    private Set<MultiplierData> extradata = Sets.newHashSet();
    private boolean enabled = false;
    private boolean queue = false;

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
        Validate.notNull(enablerName, "The enabler name can't be null");
        data.setEnablerName(enablerName);
        return this;
    }

    public MultiplierBuilder setEnablerUUID(UUID uuid) {
        data.setEnablerUUID(uuid);
        return this;
    }

    public MultiplierBuilder setExtraData(Set<MultiplierData> extradata) {
        this.extradata = extradata;
        return this;
    }

    public MultiplierBuilder addExtraData(MultiplierData data) {
        this.extradata.add(data);
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
        if (server == null) {
            multiplier.setType(MultiplierType.GLOBAL);
        } else {
            multiplier.setType(type);
        }
        multiplier.setId(id);
        multiplier.setExtradata(extradata);
        multiplier.setQueue(queue);
	multiplier.setEnablerName(data.getEnablerName());
	multiplier.setEnablerUUID(data.getEnablerUUID());
        if (enabled) {
            multiplier.enable(data.getEnablerUUID(), data.getEnablerName(), queue);
        }
        return multiplier;
    }
}
