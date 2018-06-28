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
package io.github.beelzebu.coins.api.utils;

import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.api.Multiplier;
import java.util.List;
import java.util.stream.Collectors;
import net.md_5.bungee.api.ChatColor;

/**
 * @author Beelzebu
 */
public final class StringUtils {

    public static final String rep(String msg) {
        if (msg == null) {
            return "";
        }
        String message = msg;
        if (CoinsAPI.getPlugin() != null && CoinsAPI.getPlugin().getConfig() != null) {
            message = message.replaceAll("%prefix%", CoinsAPI.getPlugin().getConfig().getString("Prefix", "&c&lCoins &6&l>&7"));
        }
        return message.replaceAll("&", "§");
    }

    public static final String rep(String msg, Multiplier multiplier) {
        String string = msg;
        if (multiplier != null) {
            string = msg.replaceAll("%enabler%", multiplier.getEnablerName()).replaceAll("%server%", multiplier.getServer()).replaceAll("%amount%", String.valueOf(multiplier.getAmount())).replaceAll("%minutes%", String.valueOf(multiplier.getMinutes())).replaceAll("%id%", String.valueOf(multiplier.getId()));
        }
        return rep(string);
    }

    public static final List<String> rep(List<String> msgs) {
        List<String> message = msgs.stream().map(StringUtils::rep).collect(Collectors.toList());
        return message;
    }

    public static final List<String> rep(List<String> msgs, Multiplier multiplierData) {
        List<String> message = msgs.stream().map(msg -> rep(msg, multiplierData)).collect(Collectors.toList());
        return message;
    }

    public static final String removeColor(String str) {
        return ChatColor.stripColor(rep(str)).replaceAll("Debug: ", "");
    }
}
