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
package io.github.beelzebu.coins.bukkit.utils.placeholders;

import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import io.github.beelzebu.coins.bukkit.utils.CompatUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * @author Beelzebu
 */
public class MultipliersPlaceholders extends PlaceholderExpansion {

    private final CoinsPlugin plugin = CoinsAPI.getPlugin();

    @Override
    public String getIdentifier() {
        return "coins-multiplier";
    }

    @Override
    public String getPlugin() {
        return "Coins";
    }

    @Override
    public String getAuthor() {
        return "Beelzebu";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player p, String placeholder) {
        if (p == null) {
            return "Player needed!";
        }
        try {
            String server = placeholder.split("_")[1];
            if (placeholder.startsWith("enabler_")) {
                String enabler = !CoinsAPI.getMultipliers(server).isEmpty() ? CoinsAPI.getMultipliers(server).stream().findFirst().get().getData().getEnablerName() : "";
                if ("".equals(enabler)) {
                    return plugin.getString("Multipliers.Placeholders.Enabler.Anyone", CompatUtils.getLocale(p));
                } else {
                    return plugin.getString("Multipliers.Placeholders.Enabler.Message", CompatUtils.getLocale(p)).replaceAll("%enabler%", enabler);
                }
            }
            if (placeholder.startsWith("amount_")) {
                return String.valueOf(!CoinsAPI.getMultipliers(server).isEmpty() ? CoinsAPI.getMultipliers(server).stream().findFirst().get().getData().getAmount() : 1);
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
