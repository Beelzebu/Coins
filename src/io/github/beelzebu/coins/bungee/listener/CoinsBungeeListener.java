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

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.md_5.bungee.api.config.ServerInfo;
import io.github.beelzebu.coins.bungee.Main;
import io.github.beelzebu.coins.common.Core;
import io.github.beelzebu.coins.common.interfaces.IConfiguration;

/**
 *
 * @author Beelzebu
 */
public abstract class CoinsBungeeListener {

    protected final Main plugin = Main.getInstance();
    protected final Core core = Core.getInstance();
    protected final IConfiguration config = core.getConfig();
    protected final List<String> message = Collections.synchronizedList(new ArrayList<>());

    public void sendExecutors(ServerInfo server) {
        config.getConfigurationSection("Command executor").forEach((String id) -> {
            synchronized (message) {
                message.clear();
                List<String> commands = config.getStringList("Command executor." + id + ".Command");
                List<String> messages = Arrays.asList(
                        id,
                        config.getString("Command executor." + id + ".Displayname", id),
                        String.valueOf(config.getDouble("Command executor." + id + ".Cost")),
                        String.valueOf(commands.size())
                );
                message.addAll(messages);
                message.addAll(messages.size(), commands);
                sendToBukkit("Coins", message, server, true);
            }
        });
    }

    public void sendToBukkit(String channel, List<String> messages, ServerInfo server, boolean wait) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(channel);
        messages.forEach((msg) -> {
            out.writeUTF(msg);
        });
        server.sendData("Coins", out.toByteArray(), wait);
    }
}
