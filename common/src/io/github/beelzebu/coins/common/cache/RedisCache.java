/**
 * This file is part of Coins
 *
 * Copyright (C) 2018 Beelzebu
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
package io.github.beelzebu.coins.common.cache;

import com.google.common.base.Preconditions;
import com.google.gson.JsonSyntaxException;
import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.api.Multiplier;
import io.github.beelzebu.coins.api.cache.CacheProvider;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import io.github.beelzebu.coins.common.messaging.RedisMessaging;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisException;

/**
 * @author Beelzebu
 */
public class RedisCache implements CacheProvider {

    private final CoinsPlugin plugin = CoinsAPI.getPlugin();
    private final RedisMessaging redis = (RedisMessaging) plugin.getMessagingService();

    @Override
    public double getCoins(UUID uuid) {
        try (Jedis jedis = redis.getPool().getResource()) {
            return getDouble(jedis.get("coins:" + uuid));
        } catch (JedisException ex) {
            plugin.log("An error has occurred getting coins for '" + uuid + "' from redis cache.");
            plugin.debug(ex);
        }
        return -1;
    }

    @Override
    public void addPlayer(UUID uuid, double coins) {
        try (Jedis jedis = redis.getPool().getResource()) {
            jedis.setex("coins:" + uuid, 1800, Double.toString(coins));
        } catch (JedisException ex) {
            plugin.log("An error has occurred adding user '" + uuid + "' to cache.");
            plugin.debug(ex);
        }
    }

    @Override
    public void removePlayer(UUID uuid) {
        try (Jedis jedis = redis.getPool().getResource()) {
            jedis.del("coins:" + uuid);
        } catch (JedisException ex) {
            plugin.log("An error has occurred removing user '" + uuid + "' from cache.");
            plugin.debug(ex);
        }
    }

    @Override
    public Multiplier getMultiplier(String server) {
        try (Jedis jedis = redis.getPool().getResource()) {
            return Multiplier.fromJson(jedis.get("multiplier:" + server), false);
        } catch (JedisException ex) {
            plugin.log("An error has occurred getting multiplier from cache for '" + server + "'");
            plugin.debug(ex);
        }
        return null;
    }

    @Override
    public void addMultiplier(String server, Multiplier multiplier) {
        try (Jedis jedis = redis.getPool().getResource()) {
            jedis.setex("multiplier:" + server, multiplier.getMinutes() * 60, multiplier.toJson().toString());
        } catch (JedisException ex) {
            plugin.log("An error has occurred adding multiplier '" + multiplier.toJson() + "' to cache.");
            plugin.debug(ex);
        }
    }

    @Override
    public void deleteMultiplier(Multiplier multiplier) {
        try (Jedis jedis = redis.getPool().getResource()) {
            jedis.del("multiplier:" + multiplier.getServer());
        } catch (JedisException ex) {
            plugin.log("An error has occurred removing multiplier '" + multiplier.toJson() + "' from cache.");
            plugin.debug(ex);
        }
    }

    @Override
    public void updateMultiplier(Multiplier multiplier, boolean callenable) {
        Preconditions.checkNotNull(multiplier, "Multiplier can't be null");
        if (callenable) {
            multiplier.enable(true);
        }
        try (Jedis jedis = redis.getPool().getResource()) {
            jedis.setex("multiplier:" + multiplier.getServer(), multiplier.getMinutes() * 60, multiplier.toJson().toString());
        } catch (JedisException ex) {
            plugin.log("An error has occurred updating multiplier '" + multiplier.toJson() + "' in the cache.");
            plugin.debug(ex);
        }
    }

    @Override
    public void addQueueMultiplier(Multiplier multiplier) {
        try (Jedis jedis = redis.getPool().getResource()) {
            jedis.setex("qmultiplier:" + multiplier.getServer(), multiplier.getMinutes() * 60, multiplier.toJson().toString());
        } catch (JedisException ex) {
            plugin.log("An error has occurred adding multiplier '" + multiplier.toJson() + "' to queue.");
            plugin.debug(ex);
        }
    }

    @Override
    public void removeQueueMultiplier(Multiplier multiplier) {
        try (Jedis jedis = redis.getPool().getResource()) {
            jedis.del("qmultiplier:" + multiplier.getServer());
        } catch (JedisException ex) {
            plugin.log("An error has occurred removing multiplier '" + multiplier.toJson() + "' from queue.");
            plugin.debug(ex);
        }
    }

    @Override
    public Set<Multiplier> getMultipliers() {
        Set<Multiplier> multipliers = new HashSet<>();
        try (Jedis jedis = redis.getPool().getResource()) {
            jedis.keys("multiplier:*").forEach(multiplier -> multipliers.add(Multiplier.fromJson(multiplier, false)));
        } catch (JedisException | JsonSyntaxException ex) {
            plugin.log("An error has occurred getting all multipliers from cache.");
            plugin.debug(ex);
        }
        return multipliers;
    }

    @Override
    public Map<UUID, Double> getPlayers() {
        Map<UUID, Double> players = new LinkedHashMap<>();
        try (Jedis jedis = redis.getPool().getResource()) {
            jedis.keys("coins:*").forEach(key -> players.put(UUID.fromString(key.split(":")[1]), getDouble(jedis.get(key))));
        } catch (JedisException ex) {
            plugin.log("An error has occurred getting all players from cache.");
            plugin.debug(ex);
        }
        return players;
    }

    private double getDouble(String string) {
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException ex) {
        }
        return -1;
    }
}
