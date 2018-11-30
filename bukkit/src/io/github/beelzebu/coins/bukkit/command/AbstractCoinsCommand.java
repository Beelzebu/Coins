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
package io.github.beelzebu.coins.bukkit.command;

import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import java.math.BigDecimal;
import org.bukkit.command.Command;

/**
 * @author Beelzebu
 */
public abstract class AbstractCoinsCommand extends Command {

    protected final CoinsPlugin plugin = CoinsAPI.getPlugin();

    AbstractCoinsCommand(String name) {
        super(name);
    }

    protected boolean isNumber(String number) {
        if (number == null) {
            return false;
        }
        try {
            BigDecimal bd = new BigDecimal(number);
            if (bd.compareTo(new BigDecimal(Double.MAX_VALUE)) <= 0 && !Double.isNaN(Double.parseDouble(number)) && !Double.isInfinite(Double.parseDouble(number))) {
                return true;
            }
        } catch (NumberFormatException ignore) {
        }
        return false;
    }
}
