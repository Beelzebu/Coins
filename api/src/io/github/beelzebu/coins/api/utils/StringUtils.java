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
package io.github.beelzebu.coins.api.utils;

import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.api.Multiplier;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.md_5.bungee.api.ChatColor;

/**
 * @author Beelzebu
 */
public final class StringUtils {

    @Nonnull
    public static String rep(String msg) {
        if (msg == null) {
            return "";
        }
        if (CoinsAPI.getPlugin() != null) {
            msg = msg.replace("%prefix%", CoinsAPI.getPlugin().getConfig().getString("Prefix", "&c&lCoins &6&l>&7"));
        }
        return msg.replace('&', ChatColor.COLOR_CHAR);
    }

    @Nonnull
    public static String rep(@Nonnull String msg, @Nonnull Multiplier multiplier) {
        return rep(msg.replace("%enabler%", multiplier.getData().getEnablerName()).replace("%server%", multiplier.getServer()).replace("%amount%", String.valueOf(multiplier.getData().getAmount())).replace("%minutes%", String.valueOf(multiplier.getData().getMinutes())).replace("%id%", String.valueOf(multiplier.getId())));
    }

    @Nonnull
    public static List<String> rep(@Nonnull List<String> lines) {
        return lines.stream().map(StringUtils::rep).collect(Collectors.toList());
    }

    @Nonnull
    public static List<String> rep(@Nonnull List<String> lines, @Nonnull Multiplier multiplierData) {
        return lines.stream().map(line -> rep(line, multiplierData)).collect(Collectors.toList());
    }

    @Nonnull
    public static String removeColor(@Nonnull String str) {
        return ChatColor.stripColor(rep(str)).replace("Debug: ", "");
    }

    public static String formatTime(long millis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        long hours = TimeUnit.MILLISECONDS.toHours(millis) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(millis));
        long days = TimeUnit.MILLISECONDS.toDays(millis);

        StringBuilder b = new StringBuilder();
        if (days > 0) {
            b.append(days);
            b.append(", ");
        }
        b.append(hours == 0 ? "00" : hours < 10 ? "0" + hours : String.valueOf(hours)).append(":");
        b.append(minutes == 0 ? "00" : minutes < 10 ? "0" + minutes : String.valueOf(minutes)).append(":");
        b.append(seconds == 0 ? "00" : seconds < 10 ? "0" + seconds : String.valueOf(seconds));
        return b.toString();
    }
}
