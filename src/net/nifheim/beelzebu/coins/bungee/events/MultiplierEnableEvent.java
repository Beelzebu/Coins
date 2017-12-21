/**
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
package net.nifheim.beelzebu.coins.bungee.events;

import java.util.UUID;
import net.md_5.bungee.api.plugin.Event;
import net.nifheim.beelzebu.coins.core.multiplier.Multiplier;

/**
 *
 * @author Beelzebu
 */
public class MultiplierEnableEvent extends Event {
    private final Multiplier data;

    public MultiplierEnableEvent(Multiplier multiplier) {
        data = multiplier;
    }

    public UUID getEnablerUUID() {
        return data.getEnablerUUID();
    }

    public Multiplier getMultiplier() {
        return data;
    }
}
