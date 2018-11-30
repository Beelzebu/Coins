/*
 * This file is part of Coins
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
package io.github.beelzebu.coins.bukkit.events;

import io.github.beelzebu.coins.api.Multiplier;
import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author Beelzebu
 */
public class MultiplierEnableEvent extends Event {

    private final static HandlerList handlers = new HandlerList();
    private final Multiplier multiplier;

    public MultiplierEnableEvent(Multiplier multiplier) {
        super(true);
        this.multiplier = multiplier;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    /**
     * Get the multiplier that fired this event.
     *
     * @return the multiplier.
     */
    public Multiplier getMultiplier() {
        return multiplier;
    }

    /**
     * Get the UUID of the enabler for this multiplier, if the multiplier wasn't enabled by a player this can be null.
     *
     * @return the uuid.
     */
    public UUID getEnablerUUID() {
        return multiplier.getData().getEnablerUUID();
    }
}
