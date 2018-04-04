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
package io.github.beelzebu.coins.bukkit.utils.placeholders;

import me.clip.placeholderapi.external.EZPlaceholderHook;
import io.github.beelzebu.coins.CoinsAPI;
import io.github.beelzebu.coins.bukkit.Main;
import io.github.beelzebu.coins.common.Core;
import org.bukkit.entity.Player;

/**
 *
 * @author Beelzebu
 */
public class MultipliersPlaceholders extends EZPlaceholderHook {

    private final Core core = Core.getInstance();

    public MultipliersPlaceholders(Main plugin) {
        super(plugin, "coins-multipliers");
    }

    @Override
    public String onPlaceholderRequest(Player p, String placeholder) {
        if (p == null) {
            return "Player needed!";
        }
        try {
            String server = placeholder.split("_")[1];
            if (placeholder.startsWith("enabler_")) {
                String enabler = CoinsAPI.getMultiplier(server) != null ? CoinsAPI.getMultiplier(server).getEnablerName() : "";
                if (enabler.equals("")) {
                    return core.getString("Multipliers.Placeholders.Enabler.Anyone", p.spigot().getLocale());
                } else {
                    return core.getString("Multipliers.Placeholders.Enabler.Message", p.spigot().getLocale()).replaceAll("%enabler%", enabler);
                }
            }
            if (placeholder.startsWith("amount_")) {
                return String.valueOf(CoinsAPI.getMultiplier(server) != null ? CoinsAPI.getMultiplier(server).getBaseData().getAmount() : 1);
            }
            if (placeholder.startsWith("time_")) {
                //return CoinsAPI.getMultiplier(server[1]).getMultiplierTimeFormated();
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        return "";
    }
}
