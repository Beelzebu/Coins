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
import com.imaginarycode.minecraft.redisbungee.events.PubSubMessageEvent;
import io.github.beelzebu.coins.Multiplier;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 *
 * @author Beelzebu
 */
public class PubSubMessageListener extends CoinsBungeeListener implements Listener {

    @EventHandler
    public void onPubSubMessage(PubSubMessageEvent e) {
        switch (e.getChannel()) {
            case "Executors":
                core.getMessagingService().getExecutors();
                break;
            case "Update":
                publishUser(core.getGson().fromJson(e.getMessage(), JsonObject.class));
                break;
            case "Multiplier":
                JsonObject message = core.getGson().fromJson(e.getMessage(), JsonObject.class);
                if (message.get("sub") != null) {
                    switch (message.get("sub").getAsString()) {
                        case "getmultipliers":
                            sendMultipliers();
                            break;
                        case "disable":
                            Multiplier multiplier = Multiplier.fromJson(message.get("message").getAsJsonObject().toString(), false);
                            disableMultiplier(multiplier);
                            break;
                    }
                } else {
                    Multiplier multiplier = Multiplier.fromJson(e.getMessage(), false);
                    handleReceivedMultiplier(multiplier);
                }
                break;
        }
    }
}
