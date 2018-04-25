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

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import io.github.beelzebu.coins.Multiplier;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 *
 * @author Beelzebu
 */
public class PluginMessageListener extends CoinsBungeeListener implements Listener {

    @EventHandler
    public void onMessageReceive(PluginMessageEvent e) {
        if (!e.getTag().equals("Coins")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(e.getData());
        String subchannel = in.readUTF();
        switch (subchannel) {
            case "Executors":
                if (plugin.useRedisBungee()) {
                    RedisBungee.getApi().sendChannelMessage(subchannel, "");
                } else {
                    core.getMessagingService().getExecutors();
                }
                break;
            case "Update":
                JsonObject data = core.getGson().fromJson(in.readUTF(), JsonObject.class);
                if (plugin.useRedisBungee()) {
                    RedisBungee.getApi().sendChannelMessage(subchannel, data.toString());
                } else {
                    publishUser(data);
                }
                break;
            case "Multiplier":
                String input = in.readUTF();
                switch (input) {
                    case "getMultipliers":
                        // update all multipliers
                        if (plugin.useRedisBungee()) {
                            sendRedisMessage("Multiplier", "getmultipliers", null);
                        } else {
                            sendMultipliers();
                        }
                        break;
                    case "disable":
                        Multiplier multiplier = Multiplier.fromJson(in.readUTF(), false);
                        if (multiplier != null) {
                            if (plugin.useRedisBungee()) {
                                sendRedisMessage("Multiplier", "disable", multiplier.toJson().toString());
                                RedisBungee.getApi().sendChannelMessage("Multiplier", "{\"sub\":\"disable\",\"message:\":" + multiplier.toJson() + "}");
                            } else {
                                disableMultiplier(multiplier);
                            }
                        }
                        break;
                    default:
                        // store the data
                        Multiplier receivedMultiplier = Multiplier.fromJson(input, false);
                        if (receivedMultiplier != null) {
                            if (plugin.useRedisBungee()) { // update in all servers if use redis
                                RedisBungee.getApi().sendChannelMessage("Multiplier", input);
                            } else { // just upadte
                                handleReceivedMultiplier(receivedMultiplier);
                            }
                        }
                        break;
                }
                break;
            case "Multiplier-Enable":
                break;
        }
    }
}
