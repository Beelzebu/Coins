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
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

/**
 * @author Beelzebu
 */
public class CoinsPlaceholders extends PlaceholderExpansion {

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
        if (placeholder.toLowerCase().matches("top_\\d")) {
            int top_n = Integer.parseInt(placeholder.toLowerCase().replace("top_", ""));
            Map<String, Double> top = CoinsAPI.getTopPlayers(top_n);
            for (int i = 0; i < top_n; i++) {
                top.remove(top.entrySet().stream().findFirst().get().getKey());
            }
            return top.entrySet().stream().findFirst().get().getKey();

        }
        switch (placeholder.toLowerCase()) {
            case "amount":
                String coinsS;
                try {
                    coinsS = CoinsAPI.getCoinsString(p.getUniqueId());
                } catch (NullPointerException ex) {
                    coinsS = "Loading...";
                }
                return coinsS;
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
