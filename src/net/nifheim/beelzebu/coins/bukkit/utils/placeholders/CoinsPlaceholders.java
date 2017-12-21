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
package net.nifheim.beelzebu.coins.bukkit.utils.placeholders;

import java.text.NumberFormat;
import java.util.Locale;
import me.clip.placeholderapi.external.EZPlaceholderHook;
import net.nifheim.beelzebu.coins.CoinsAPI;
import net.nifheim.beelzebu.coins.bukkit.Main;
import org.bukkit.entity.Player;

/**
 *
 * @author Beelzebu
 */
public class CoinsPlaceholders extends EZPlaceholderHook {

    public CoinsPlaceholders(Main main) {
        super(main, "coins");
    }

    @Override
    public String onPlaceholderRequest(Player p, String coins) {
        if (p == null) {
            return "Player needed!";
        }
        switch (coins) {
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
        return "0";
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
