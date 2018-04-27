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
package io.github.beelzebu.coins.bukkit.messaging;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import io.github.beelzebu.coins.CoinsAPI;
import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.MultiplierType;
import io.github.beelzebu.coins.bukkit.Main;
import io.github.beelzebu.coins.common.CacheManager;
import io.github.beelzebu.coins.common.executor.Executor;
import io.github.beelzebu.coins.common.messaging.BungeeMessaging;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 *
 * @author Beelzebu
 */
public final class BukkitBungeeMessaging extends BungeeMessaging implements PluginMessageListener {

    @Override
    public void publishUser(UUID uuid, double coins) {
        sendMessage("Coins", "Update", Collections.singletonList("{\"uuid\":" + uuid + ",\"coins\":" + coins + "}"), false);
    }

    @Override
    public void getExecutors() {
        sendMessage("Coins", "Executors", null, false);
    }

    @Override
    protected void sendMessage(String channel, String subchannel, List<String> messages, boolean wait) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(subchannel);
        messages.forEach(message -> out.writeUTF(message));
        Player p = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (p != null) {
            try {
                p.sendPluginMessage(Main.getInstance(), channel, out.toByteArray());
            } catch (Exception ex) {
                core.log("Hey, you need to install the plugin in BungeeCord if you have bungeecord enabled in spigot.yml!");
            }
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("Coins")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        switch (subchannel) {
            case "Executors":
                String executor = in.readUTF();
                Executor ex = Executor.fromJson(executor);
                if (core.getExecutorManager().getExecutor(ex.getId()) == null) {
                    core.getExecutorManager().addExecutor(ex);
                    core.log("The executor " + ex.getId() + " was received from BungeeCord.");
                    core.debug("ID: " + ex.getId());
                    core.debug("Displayname: " + ex.getDisplayname());
                    core.debug("Cost: " + ex.getCost());
                    core.debug("Commands: ");
                    ex.getCommands().forEach((command) -> {
                        core.debug(command);
                    });
                } else {
                    core.debug("An executor with the id: " + ex.getId() + " was received from BungeeCord but a local Executor with that id already exists.");
                }
                break;
            case "Update":
                JsonObject data = core.getGson().fromJson(in.readUTF(), JsonObject.class);
                if (data != null) {
                    UUID puuid = UUID.fromString(data.get("uuid").getAsString());
                    if (CacheManager.getCoins(puuid) > -1) {
                        CacheManager.updateCoins(puuid, data.get("coins").getAsDouble());
                    }
                }
                break;
            case "Multiplier": {
                Multiplier multiplier = Multiplier.fromJson(in.readUTF(), false);
                core.debug("A multiplier was received from BungeeCord: " + multiplier.toJson());
                if (CoinsAPI.getMultiplier() != null && multiplier.getId() == CoinsAPI.getMultiplier().getId() && multiplier.getEndTime() == CoinsAPI.getMultiplier().getEndTime()) {
                    return;
                }
                if (multiplier.getType().equals(MultiplierType.GLOBAL)) {
                    multiplier.setServer(core.getConfig().getServerName().toLowerCase());
                }
                if (multiplier.getServer().equals(core.getConfig().getServerName()) && !multiplier.isEnabled()) {
                    if (multiplier.isQueue()) {
                        CacheManager.getQueuedMultipliers().add(multiplier);
                    } else { // if the multiplier isn't enabled and isn't for queue it must be a disable request.
                        CoinsAPI.getMultiplier().disable();
                    }
                }
                CacheManager.addMultiplier(multiplier.getServer(), multiplier);
            }
            break;
            case "Multiplier-Enable": {
                Multiplier multiplier = Multiplier.fromJson(in.readUTF(), true);
                core.debug("A multiplier enable request was received from BungeeCord: " + multiplier.toJson());
            }
            break;
        }
    }
}
