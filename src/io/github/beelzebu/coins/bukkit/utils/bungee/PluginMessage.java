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
package io.github.beelzebu.coins.bukkit.utils.bungee;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.github.beelzebu.coins.CoinsAPI;
import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.MultiplierType;
import io.github.beelzebu.coins.bukkit.Main;
import io.github.beelzebu.coins.common.CacheManager;
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.executor.Executor;
import java.util.ArrayList;
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
public class PluginMessage implements PluginMessageListener {

    private final CoinsCore core = CoinsCore.getInstance();

    @Override
    public synchronized void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("Coins")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subchannel = in.readUTF();
        switch (subchannel) {
            case "Coins":
                String id = in.readUTF();
                String displayname = in.readUTF();
                double cost = Double.parseDouble(in.readUTF());
                int cmds = Integer.parseInt(in.readUTF());
                List<String> commands = new ArrayList<>();
                if (cmds > 0) {
                    for (int i = 0; i < cmds; i++) {
                        commands.add(in.readUTF());
                    }
                }
                Executor ex = new Executor(id, displayname, cost, commands);
                if (core.getExecutorManager().getExecutor(id) == null) {
                    core.getExecutorManager().addExecutor(ex);
                    core.log("The executor " + ex.getID() + " was received from BungeeCord.");
                    core.debug("ID: " + ex.getID());
                    core.debug("Displayname: " + ex.getDisplayName());
                    core.debug("Cost: " + ex.getCost());
                    core.debug("Commands: ");
                    ex.getCommands().forEach((command) -> {
                        core.debug(command);
                    });
                } else {
                    core.debug("An executor with the id: " + ex.getID() + " was received from BungeeCord but a local Executor with that id already exists.");
                }
                break;
            case "Update":
                String data = in.readUTF();
                if (data.split(" ").length == 2) {
                    UUID puuid = UUID.fromString(data.split(" ")[0]);
                    if (CacheManager.getCoins(puuid) > -1) {
                        CacheManager.updateCoins(puuid, Double.parseDouble(data.split(" ")[1]));
                    }
                }
                break;
            case "Multiplier":
                String multdata = in.readUTF();
                core.debug(multdata);
                Multiplier multiplier = Multiplier.fromJson(multdata, false);
                if (multiplier.getType().equals(MultiplierType.GLOBAL)) {
                    multiplier.setServer(core.getConfig().getServerName().toLowerCase());
                }
                if (multiplier.getId() != CoinsAPI.getMultiplier().getId()) {
                    CacheManager.addMultiplier(multiplier.getServer(), multiplier);
                } else if (multiplier.isQueue()) {
                    CacheManager.getQueuedMultipliers().add(multiplier);
                }
                break;
            default:
                break;
        }
    }

    public void sendToBungeeCord(String channel, String message) {
        sendToBungeeCord(channel, Collections.singletonList(message));
    }

    public void sendToBungeeCord(String channel, List<String> messages) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(channel);
        messages.forEach(message -> {
            out.writeUTF(message);
        });
        Player p = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (p != null) {
            try {
                Bukkit.getServer().sendPluginMessage(Main.getInstance(), "Coins", out.toByteArray());
            } catch (Exception ex) {
                core.log("Hey, you need to install the plugin in BungeeCord if you have bungeecord enabled in spigot.yml!");
            }
        }
    }
}
