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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * @author Beelzebu
 */
public class CoinsPlaceholders extends PlaceholderExpansion {

    private final DecimalFormat decimalFormat = new DecimalFormat("#.#");

    @Override
    public String getIdentifier() {
        return "coins";
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
        if (placeholder.toLowerCase().startsWith("top_") && placeholder.toLowerCase().matches("\\d+$")) {
            int top = Integer.parseInt(placeholder.toLowerCase().replaceAll("[^\\d+$]", ""));
            if (placeholder.toLowerCase().matches("^top_name_\\d+$")) {
                return decimalFormat.format(CoinsAPI.getTopPlayers(top)[top - 1].getName());
            }
            if (placeholder.toLowerCase().matches("^top_balance_\\d+$")) {
                return decimalFormat.format(CoinsAPI.getTopPlayers(top)[top - 1].getCoins());
            }
        }
        switch (placeholder.toLowerCase()) {
            case "amount":
                String coinsString;
                try {
                    coinsString = CoinsAPI.getCoinsString(p.getUniqueId());
                } catch (NullPointerException ex) {
                    coinsString = "Loading...";
                }
                return coinsString;
            case "amount_formatted":
                return fix(CoinsAPI.getCoins(p.getUniqueId()));
            default:
                break;
        }
        return "<invalid placeholder>";
    }

    private String format(double d) {
        NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(0);
        return format.format(d);
    }

    private String fix(double d) {
        if (d < 1000D) {
            return format(d);
        } else if (d < 1000000D) {
            return format(d / 1000D) + "k";
        } else if (d < 1.0E9D) {
            return format(d / 1000000D) + "m";
        } else if (d < 1.0E12D) {
            return format(d / 1.0E9D) + "b";
        } else if (d < 1.0E15D) {
            return format(d / 1.0E12D) + "t";
        } else if (d < 1.0E18D) {
            return format(d / 1.0E15D) + "t";
        } else {
            long send = (long) d;
            return String.valueOf(send);
        }
    }
}
