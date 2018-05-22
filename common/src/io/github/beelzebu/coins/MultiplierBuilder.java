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
package io.github.beelzebu.coins;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.Set;
import java.util.UUID;

/**
 * Class to build multipliers.
 *
 * @author Beelzebu
 * @deprecated internal use only
 */
@Deprecated
public final class MultiplierBuilder {

    private final String server;
    private final MultiplierType type;
    private final MultiplierData data;
    private int id = -1;
    private Set<MultiplierData> extradata = Sets.newHashSet();
    private boolean enabled = false;
    private boolean queue = false;
    private long endtime = 0L;

    private MultiplierBuilder(String server, MultiplierType type, MultiplierData data) {
        Preconditions.checkNotNull(server, "Server can't be null.");
        Preconditions.checkNotNull(type, "MultiplierType can't be null.");
        Preconditions.checkNotNull(data, "MultiplierData can't be null.");
        this.server = server;
        this.type = type;
        this.data = data;
    }

    public static MultiplierBuilder newBuilder(String server, MultiplierType type, MultiplierData data) {
        return new MultiplierBuilder(server, type, data);
    }

    public MultiplierBuilder setID(int id) {
        this.id = id;
        return this;
    }

    public MultiplierBuilder setEnablerName(String enablerName) {
        Preconditions.checkNotNull(enablerName, "The enabler name can't be null");
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

    public MultiplierBuilder setEndTime(long endtime) {
        this.endtime = endtime;
        return this;
    }

    public Multiplier build(boolean callenable) {
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
        multiplier.setEndTime(endtime);
        if (enabled && callenable) {
            multiplier.enable(data.getEnablerUUID(), data.getEnablerName(), queue);
        }
        return multiplier;
    }
}
