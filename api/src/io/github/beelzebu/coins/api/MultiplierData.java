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
package io.github.beelzebu.coins.api;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Beelzebu
 */
@Getter
@Setter
@AllArgsConstructor
public class MultiplierData {

    /**
     * Get the UUID of who enabled this multiplier
     *
     * @return UUID of who enabled this multiplier, never should be null unless specified by other plugin.
     */
    private UUID enablerUUID = UUID.randomUUID();
    /**
     * Get the username of who enabled this multiplier
     *
     * @return username of who enabled this multiplier, never should be null unless specified by other plugin.
     */
    private String enablerName = "SERVER";
    private int amount = 1;
    private int minutes = 60;

    public MultiplierData(int amount, int minutes) {
        this.amount = amount;
        this.minutes = minutes;
    }
}
