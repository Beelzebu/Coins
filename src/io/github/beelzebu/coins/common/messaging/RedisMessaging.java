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
package io.github.beelzebu.coins.common.messaging;

import com.google.gson.JsonObject;
import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.MultiplierType;
import io.github.beelzebu.coins.common.CacheManager;
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.executor.Executor;
import io.github.beelzebu.coins.common.interfaces.IMessagingService;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

/**
 *
 * @author Beelzebu
 */
@NoArgsConstructor(access = AccessLevel.NONE)
public class RedisMessaging implements IMessagingService {

    private final CoinsCore core = CoinsCore.getInstance();
    private JedisPool pool;
    private PubSubListener psl;

    @Override
    public void start() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMinIdle(1);
        String host = core.getConfig().getString("Redis.Host", "localhost");
        int port = core.getConfig().getInt("Redis.Port", 6379);
        String password = core.getConfig().getString("Redis.Password");
        if (password != null && !"".equals(password)) {
            pool = new JedisPool(config, host, port, 0, password);
        } else {
            pool = new JedisPool(config, host, port);
        }
        core.getMethods().runAsync(psl = new PubSubListener());
    }

    @Override
    public MessagingServiceType getType() {
        return MessagingServiceType.REDIS;
    }

    @Override
    public void publishUser(UUID uuid, double coins) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish("coins-data-update", "{\"uuid\":" + uuid + ",\"coins\":" + coins + "}");
        }
    }

    @Override
    public void publishMultiplier(Multiplier multiplier) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish("coins-multiplier", multiplier.toJson().toString());
        }
    }

    @Override
    public void enableMultiplier(Multiplier multiplier) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish("coins-event", "{\"event\":\"MultiplierEnableEvent\",\"multiplier\":" + multiplier.toJson() + "}");
        }
    }

    @Override
    public void getMultipliers() {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish("coins-multiplier", "get");
        }
    }

    @Override
    public void getExecutors() {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish("coins-executors", "get");
        }
    }

    @Override
    public void stop() {
        psl.poison();
        pool.close();
        pool.destroy();
    }

    private void sendExecutor(Executor ex) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish("coins-executors", ex.toJson());
        }
    }

    private void sendMultiplier(Multiplier multiplier) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish("coins-multiplier", multiplier.toJson().toString());
        }
    }

    @NoArgsConstructor(access = AccessLevel.NONE)
    private class PubSubListener implements Runnable {

        private JedisPubSub jpsh = new JedisPubSubHandler();

        @Override
        public void run() {
            boolean broken = false;
            try (Jedis rsc = pool.getResource()) {
                try {
                    jpsh = new JedisPubSubHandler();
                    rsc.subscribe(jpsh, "coins-data-update", "coins-executors", "coins-multiplier", "coins-multiplier-disable", "coins-event");
                } catch (Exception e) {
                    core.log("PubSub error, attempting to recover.");
                    try {
                        jpsh.unsubscribe();
                    } catch (Exception ignore) {
                    }
                    broken = true;
                }
            }
            if (broken) {
                run();
            }
        }

        public void addChannel(String... channel) {
            jpsh.subscribe(channel);
        }

        public void removeChannel(String... channel) {
            jpsh.unsubscribe(channel);
        }

        public void poison() {
            jpsh.unsubscribe();
        }
    }

    @NoArgsConstructor(access = AccessLevel.NONE)
    private class JedisPubSubHandler extends JedisPubSub {

        @Override
        public void onMessage(String channel, String message) {
            core.debug("Redis Log: Recived a message in channel: " + channel);
            core.debug("Redis Log: Message is:");
            core.debug(message);
            switch (channel) {
                case "coins-data-update":
                    JsonObject data = core.getGson().fromJson(message, JsonObject.class);
                    CacheManager.updateCoins(UUID.fromString(data.get("uuid").getAsString()), data.get("coins").getAsDouble());
                    break;
                case "coins-executors":
                    if (message.equals("get")) {
                        core.getExecutorManager().getExecutors().forEach(exec -> sendExecutor(exec));
                    } else {
                        Executor ex = Executor.fromJson(message);
                        if (core.getExecutorManager().getExecutor(ex.getId()) == null) {
                            core.getExecutorManager().addExecutor(ex);
                            core.log("The executor " + ex.getId() + " was received from Redis PubSub.");
                            core.debug("ID: " + ex.getId());
                            core.debug("Displayname: " + ex.getDisplayname());
                            core.debug("Cost: " + ex.getCost());
                            core.debug("Commands: ");
                            ex.getCommands().forEach(command -> core.debug(command));
                        } else {
                            core.debug("An executor with the id: " + ex.getId() + " was received from Redis but a local Executor with that id already exists.");
                        }
                    }
                    break;
                case "coins-multiplier":
                    if (message.equals("get")) {
                        CacheManager.getMultipliersData().asMap().values().forEach(multiplier -> sendMultiplier(multiplier));
                    } else {
                        Multiplier multiplier = Multiplier.fromJson(message, false);
                        if (multiplier.getType().equals(MultiplierType.GLOBAL)) {
                            multiplier.setServer(core.getConfig().getServerName());
                        }
                        CacheManager.addMultiplier(multiplier.getServer(), multiplier);
                    }
                    break;
                case "coins-multiplier-disable":
                    Multiplier.fromJson(message, false).disable();
                    break;
                case "coins-event":
                    JsonObject event = core.getGson().fromJson(message, JsonObject.class);
                    switch (event.get("event").getAsString()) {
                        case "MultiplierEnableEvent":
                            core.getMethods().callMultiplierEnableEvent(Multiplier.fromJson(event.getAsJsonObject("multiplier").toString(), false));
                            break;
                        default:
                            core.debug("Invalid redis event");
                            break;
                    }
                    break;
                default:
                    core.debug("Invalid redis channel");
                    break;
            }
        }
    }
}
