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
import io.github.beelzebu.coins.common.messaging.ProxyMessaging;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 *
 * @author Beelzebu
 */
public final class BukkitMessaging extends ProxyMessaging implements PluginMessageListener {

    @Override
    public void start() {
        Bukkit.getMessenger().registerOutgoingPluginChannel((Plugin) core.getBootstrap(), CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel((Plugin) core.getBootstrap(), CHANNEL, this);
    }

    @Override
    public void stop() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel((Plugin) core.getBootstrap(), CHANNEL, this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel((Plugin) core.getBootstrap(), CHANNEL);
    }

    @Override
    protected void sendMessage(String message, boolean wait) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(message);
        Player p = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (p != null) {
            try {
                p.sendPluginMessage((Plugin) core.getBootstrap(), CHANNEL, out.toByteArray());
            } catch (Exception ex) {
                core.log("Hey, you need to install the plugin in BungeeCord if you have bungeecord enabled in spigot.yml!");
            }
        } else {
            core.log("Trying to send a message without players, bungee messaging needs at least one player to send messages "
                    + "the data of this message may be lost or can cause concurrency problems, is recommended to use Redis as "
                    + "messaging service due it doesn't have this limitation and avoid this kind of problems.");
            core.log("Message: " + message);
        }
    }

    @Override
    protected void sendMessage(JsonObject message) {
        sendMessage(message.toString(), false);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        JsonObject data = core.getGson().fromJson(in.readUTF(), JsonObject.class);
        handleMessage(data);
    }
}
