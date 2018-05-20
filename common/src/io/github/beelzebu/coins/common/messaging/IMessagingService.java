/**
 * This file is part of Coins
 *
 * Copyright © 2018 Beelzebu
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero Generalpublic abstract License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero Generalpublic abstract License for more
 * details.
 *
 * You should have received a copy of the GNU Affero Generalpublic abstract License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.beelzebu.coins.common.messaging;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.common.CacheManager;
import io.github.beelzebu.coins.common.CoinsCore;
import java.util.LinkedHashSet;
import java.util.UUID;

/**
 *
 * @author Beelzebu
 */
public abstract class IMessagingService {

    protected final CoinsCore core = CoinsCore.getInstance();
    protected final LinkedHashSet<UUID> messages = new LinkedHashSet<>();

    /**
     * Start this messaging service.
     */
    public abstract void start();

    /**
     * Get the messaging service type in use.
     *
     * @return messaging service type defined by implementing classes.
     */
    public abstract MessagingService getType();

    /**
     * Stop and shutdown this messaging service instance.
     */
    public abstract void stop();

    /**
     * Publish user coins over all servers using this messaging service.
     *
     * @param uuid user to publish.
     * @param coins coins to publish.
     */
    public final void publishUser(UUID uuid, double coins) {
        Preconditions.checkNotNull(uuid, "UUID can't be null");
        if (coins > -1) {
            try {
                CacheManager.getPlayersData().put(uuid, coins);
                core.debug("Updated local data for: " + uuid);
                if (!getType().equals(MessagingService.NONE)) {
                    sendMessage("{\"uuid\":\"" + uuid + "\",\"coins\":" + coins + "}", MessageType.USER_UPDATE);
                }
            } catch (Exception ex) {
                core.log("An unexpected error has ocurred while updating coins for: " + uuid);
                core.log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins/issues");
                core.debug(ex);
            }
        }
    }

    /**
     * Publish a multiplier over all servers using this messaging service.
     *
     * @param multiplier -
     */
    public final void updateMultiplier(Multiplier multiplier) {
        Preconditions.checkNotNull(multiplier, "Multiplier can't be null");
        try {
            CacheManager.addMultiplier(multiplier.getServer(), multiplier);
            if (!getType().equals(MessagingService.NONE)) {
                JsonObject message = new JsonObject();
                message.add("multiplier", multiplier.toJson());
                sendMessage(message.toString(), MessageType.MULTIPLIER_UPDATE);
            }
        } catch (Exception ex) {
            core.log("An unexpected error has ocurred while publishing a multiplier over messaging service.");
            core.log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins/issues");
            core.debug(ex);
        }
    }

    /**
     * Enable a multiplier in all servers using this messaging service.
     *
     * @param multiplier -
     */
    public final void enableMultiplier(Multiplier multiplier) {

    }

    public final void disableMultiplier(Multiplier multiplier) {

    }

    /**
     * Send a request to get all multipliers from other servers using this
     * messaging service, if this server is spigot will request it to bungeecord
     * and viceversa.
     */
    public abstract void getMultipliers();

    /**
     * Send a request to get all executors from bungeecord or other bungeecord
     * instances if you're using more than one bungeecord server.
     */
    public abstract void getExecutors();

    /**
     * Sub classes must override this to send the message so we can handle it
     * before
     *
     * @param message JSON message to send
     */
    protected abstract void sendMessage(JsonObject message);

    /**
     * Send a message in JSON format using this messaging service
     *
     * @param message what we should send
     * @param type message type
     */
    protected final void sendMessage(String message, MessageType type) {
        try {
            JsonObject jobj = core.getGson().fromJson(message, JsonObject.class);
            UUID uuid = UUID.randomUUID();
            messages.add(uuid);
            jobj.addProperty("messageid", uuid.toString());
            jobj.addProperty("type", type.toString());
            sendMessage(jobj);
        } catch (JsonSyntaxException ex) {
            core.log("Trying to send an invalid JSON message: " + message);
            core.log(ex.getMessage());
        }
    }

    protected final void handleMessage(JsonObject message) {
        core.debug("&6Messaging Log: &7Recived a message from another server, message is: " + message);
        UUID messageid = UUID.fromString(message.get("messageid").getAsString());
        if (messages.contains(messageid)) { // the message was sent from this server so don't read it
            messages.remove(messageid);
            return;
        }
        MessageType type = MessageType.valueOf(message.get("type").getAsString());
        switch (type) {
            case USER_UPDATE: {
                UUID uuid = UUID.fromString(message.get("uuid").getAsString());
                double coins = message.get("coins").getAsDouble();
                core.getDatabase().setCoins(uuid, coins);
                CacheManager.getPlayersData().put(uuid, coins);
            }
            break;
            case MULTIPLIER_UPDATE: {

            }
            break;
        }
    }

    protected enum MessageType {
        USER_UPDATE,
        MULTIPLIER_UPDATE,
        MULTIPLIER_ENABLE,
        MULTIPLIER_DISABLE;
    }
}
