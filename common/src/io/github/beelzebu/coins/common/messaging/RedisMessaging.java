/*
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
import io.github.beelzebu.coins.api.messaging.AbstractMessagingService;
import io.github.beelzebu.coins.api.messaging.MessagingServiceType;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

/**
 * @author Beelzebu
 */
public class RedisMessaging extends AbstractMessagingService {

    @Getter
    private JedisPool pool;
    private PubSubListener psl;

    @Override
    public void start() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMinIdle(1);
        String host = plugin.getConfig().getString("Redis.Host", "localhost");
        int port = plugin.getConfig().getInt("Redis.Port", 6379);
        String password = plugin.getConfig().getString("Redis.Password");
        if (Objects.nonNull(password) && !Objects.equals(password, "")) {
            pool = new JedisPool(config, host, port, 0, password);
        } else {
            pool = new JedisPool(config, host, port);
        }
        plugin.getBootstrap().runAsync(psl = new PubSubListener(new JedisPubSubHandler()));
    }

    @Override
    public MessagingServiceType getType() {
        return MessagingServiceType.REDIS;
    }

    @Override
    public void stop() {
        psl.poison();
        pool.close();
        pool.destroy();
        pool = null;
    }

    @Override
    public void publishUser(UUID uuid, double coins) {
        plugin.getCache().updatePlayer(uuid, coins);
    }

    @Override
    protected void sendMessage(JsonObject message) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish("coins-messaging", message.toString());
        }
    }

    @AllArgsConstructor
    private class PubSubListener implements Runnable {

        private final JedisPubSub jpsh;

        @Override
        public void run() {
            boolean broken = false;
            try (Jedis rsc = pool.getResource()) {
                try {
                    rsc.subscribe(jpsh, "coins-messaging");
                } catch (Exception e) {
                    plugin.log("PubSub error, attempting to recover.");
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

        void poison() {
            jpsh.unsubscribe();
        }
    }

    private class JedisPubSubHandler extends JedisPubSub {

        @Override
        public void onMessage(String channel, String message) {
            handleMessage(plugin.getGson().fromJson(message, JsonObject.class));
        }
    }
}
