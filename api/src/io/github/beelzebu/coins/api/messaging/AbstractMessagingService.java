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
package io.github.beelzebu.coins.api.messaging;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.api.Multiplier;
import io.github.beelzebu.coins.api.MultiplierType;
import io.github.beelzebu.coins.api.executor.Executor;
import io.github.beelzebu.coins.api.executor.ExecutorManager;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import io.github.beelzebu.coins.api.storage.StorageType;
import java.util.LinkedHashSet;
import java.util.UUID;

/**
 * @author Beelzebu
 */
public abstract class AbstractMessagingService {

    protected final CoinsPlugin plugin = CoinsAPI.getPlugin();
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
     * @param uuid  user to publish.
     * @param coins coins to publish.
     */
    public final void publishUser(UUID uuid, double coins) {
        Preconditions.checkNotNull(uuid, "UUID can't be null");
        if (coins > -1) {
            try {
                plugin.getCache().addPlayer(uuid, coins);
                plugin.debug("Updated local data for: " + uuid);
                if (!getType().equals(MessagingService.NONE)) {
                    JsonObject user = new JsonObject();
                    user.addProperty("uuid", uuid.toString());
                    user.addProperty("coins", coins);
                    sendMessage(user, MessageType.USER_UPDATE);
                }
            } catch (Exception ex) {
                plugin.log("An unexpected error has occurred while updating coins for: " + uuid);
                plugin.log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins/issues");
                plugin.debug(ex);
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
            plugin.getCache().addMultiplier(multiplier.getServer(), multiplier);
            sendMessage(objectWith("multiplier", multiplier.toJson()), MessageType.MULTIPLIER_UPDATE);
        } catch (Exception ex) {
            plugin.log("An unexpected error has occurred while publishing a multiplier over messaging service.");
            plugin.log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins/issues");
            plugin.debug(ex);
        }
    }

    /**
     * Enable a multiplier in all servers using this messaging service.
     *
     * @param multiplier -
     */
    public final void enableMultiplier(Multiplier multiplier) {
        Preconditions.checkNotNull(multiplier, "Multiplier can't be null");
        try {
            plugin.getCache().addMultiplier(multiplier.getServer(), multiplier);
            sendMessage(add(objectWith("multiplier", multiplier.toJson()), "enable", true), MessageType.MULTIPLIER_UPDATE);
        } catch (Exception ex) {
            plugin.log("An unexpected error has occurred while enabling a multiplier over messaging service.");
            plugin.log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins/issues");
            plugin.debug(ex);
        }
    }

    /**
     * Disable a multiplier in all servers using this messaging service.
     *
     * @param multiplier -
     */
    public final void disableMultiplier(Multiplier multiplier) {
        Preconditions.checkNotNull(multiplier, "Multiplier can't be null");
        try {
            plugin.getCache().addMultiplier(multiplier.getServer(), multiplier);
            sendMessage(objectWith("multiplier", multiplier.toJson()), MessageType.MULTIPLIER_DISABLE);
        } catch (Exception ex) {
            plugin.log("An unexpected error has occurred while disabling a multiplier over messaging service.");
            plugin.log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins/issues");
            plugin.debug(ex);
        }
    }

    /**
     * Send a request to get all multipliers from other servers using this
     * messaging service, if this server is spigot will request it to bungeecord
     * and viceversa.
     */
    public final void getMultipliers() {
        sendMessage(new JsonObject(), MessageType.MULTIPLIER_UPDATE);
    }

    /**
     * Send a request to get all executors from bungeecord or other bungeecord
     * instances if you're using more than one bungeecord server.
     */
    public final void getExecutors() {
        sendMessage(new JsonObject(), MessageType.GET_EXECUTORS);
    }

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
     * @param type    message type
     */
    protected final void sendMessage(JsonObject message, MessageType type) {
        if (getType().equals(MessagingService.NONE)) {
            return;
        }
        JsonObject jobj = message;
        UUID uuid = UUID.randomUUID();
        messages.add(uuid);
        jobj.addProperty("messageid", uuid.toString());
        jobj.addProperty("type", type.toString());
        sendMessage(jobj);
    }

    protected final void handleMessage(JsonObject message) {
        plugin.debug("&6Messaging Log: &7Recived a message from another server, message is: " + message);
        UUID messageid = UUID.fromString(message.get("messageid").getAsString());
        if (messages.contains(messageid)) { // the message was sent from this server so don't read it
            plugin.debug("&6Messaging Log: &7Message was sent from this server, ignoring.");
            messages.remove(messageid);
            return;
        }
        MessageType type = MessageType.valueOf(message.get("type").getAsString());
        switch (type) {
            case USER_UPDATE: {
                UUID uuid = UUID.fromString(message.get("uuid").getAsString());
                double coins = message.get("coins").getAsDouble();
                plugin.getDatabase().setCoins(uuid, coins);
                plugin.getCache().addPlayer(uuid, coins);
            }
            break;
            case GET_EXECUTORS: {
                if (message.has("executor")) {
                    ExecutorManager.addExecutor(Executor.fromJson(message.getAsJsonObject("executor").toString()));
                } else {
                    plugin.getBootstrap().getPlugin().loadExecutors();
                    ExecutorManager.getExecutors().forEach(ex -> sendMessage(objectWith("executor", ex.toJson()), type));
                }
            }
            break;
            case MULTIPLIER_UPDATE: {
                if (message.has("multiplier")) {
                    Multiplier multiplier = Multiplier.fromJson(message.getAsJsonObject("multiplier").toString(), message.has("enable"));
                    plugin.getCache().addMultiplier(multiplier.getType().equals(MultiplierType.GLOBAL) ? plugin.getConfig().getServerName() : multiplier.getServer(), multiplier);
                } else {
                    plugin.getCache().getMultipliers().forEach(multiplier -> sendMessage(objectWith("multiplier", multiplier.toJson()), type));
                }
            }
            break;
            case MULTIPLIER_DISABLE: {
                Multiplier multiplier = Multiplier.fromJson(message.get("multiplier").getAsString(), false);
                plugin.getCache().deleteMultiplier(multiplier); // remove from the local cache and storage
                if (plugin.getStorageType().equals(StorageType.SQLITE)) {// may be it wasn't removed from this database
                    plugin.getDatabase().deleteMultiplier(multiplier);
                }
            }
            break;
        }
    }

    // simple method to use one line lambda expressions when handling messages
    private JsonObject objectWith(String key, JsonObject value) {
        JsonObject jobj = new JsonObject();
        jobj.add(key, value);
        return jobj;
    }

    private JsonObject add(JsonObject jobj, String key, Object value) {
        jobj.addProperty(key, value.toString());
        return jobj;
    }

}
