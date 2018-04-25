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
package io.github.beelzebu.coins.bungee.listener;

import com.google.gson.JsonObject;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import io.github.beelzebu.coins.CoinsAPI;
import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.bungee.Main;
import io.github.beelzebu.coins.common.CacheManager;
import io.github.beelzebu.coins.common.CoinsCore;
import java.util.Iterator;
import java.util.UUID;

/**
 *
 * @author Beelzebu
 */
public abstract class CoinsBungeeListener {

    protected final Main plugin = Main.getInstance();
    protected final CoinsCore core = CoinsCore.getInstance();

    protected void publishUser(JsonObject data) {
        if (data != null) {
            UUID uuid = UUID.fromString(data.get("uuid").getAsString());
            double coins = data.get("coins").getAsDouble();
            CacheManager.updateCoins(uuid, coins);
            core.getMessagingService().publishUser(uuid, coins);
        }
    }

    protected void disableMultiplier(Multiplier multiplier) {
        Multiplier localMultiplier = CacheManager.getMultiplier(multiplier.getServer());
        multiplier.disable();
        if (localMultiplier != null) {
            multiplier.disable();
        } else {
            core.getMessagingService().publishMultiplier(multiplier);
        }
    }

    protected void handleReceivedMultiplier(Multiplier multiplier) {
        CacheManager.addMultiplier(multiplier.getServer(), multiplier);
        core.getMessagingService().publishMultiplier(multiplier);
    }

    protected void enableMultiplier(Multiplier multiplier) {
        Multiplier current = CoinsAPI.getMultiplier(multiplier.getServer());
        if (current != null) {
            current.disable();
        }
        CacheManager.addMultiplier(multiplier.getServer(), multiplier);
    }

    protected void sendMultipliers() {
        Iterator<String> it = CacheManager.getMultipliersData().asMap().keySet().iterator();
        while (it.hasNext()) {
            String server = it.next();
            CacheManager.updateMultiplier(CacheManager.getMultiplier(server), true);
        }
    }

    protected void sendRedisMessage(String channel, String subchannel, String message) {
        RedisBungee.getApi().sendChannelMessage(channel, "{\"sub\":\"" + subchannel + "\",\"message\":\"" + message + "\"}");
    }
}
