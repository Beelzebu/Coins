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
package net.nifheim.beelzebu.coins.core.database;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.nifheim.beelzebu.coins.CoinsAPI;
import net.nifheim.beelzebu.coins.CoinsResponse;
import net.nifheim.beelzebu.coins.CoinsResponse.CoinsResponseType;
import net.nifheim.beelzebu.coins.core.CacheManager;
import net.nifheim.beelzebu.coins.core.multiplier.Multiplier;
import net.nifheim.beelzebu.coins.core.multiplier.MultiplierBuilder;
import net.nifheim.beelzebu.coins.core.multiplier.MultiplierData;
import net.nifheim.beelzebu.coins.core.multiplier.MultiplierType;
import org.apache.commons.lang.Validate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

/**
 *
 * @author Beelzebu
 */
public class Redis implements CoinsDatabase {

    @Getter
    private JedisPool pool;
    private PubSubListener psl;

    @Override
    public void setup() {
        pool = new JedisPool(new JedisPoolConfig(), core.getConfig().getString("Redis.Host"), core.getConfig().getInt("Redis.Port"), 0, core.getConfig().getString("Redis.Password"));
        core.getMethods().runAsync(psl = new PubSubListener());
    }

    @Override
    public double getCoins(UUID uuid) {
        double coins = -1;
        try (Jedis jedis = pool.getResource()) {
            if (CoinsAPI.isindb(uuid)) {
                coins = Double.valueOf(jedis.hget("coins_data", uuid.toString()));
            } else if (core.isOnline(uuid)) {
                coins = core.getConfig().getDouble("General.Starting Coins", 0);
                createPlayer(uuid, core.getNick(uuid, false).toLowerCase(), coins);
            }
        }
        return coins;
    }

    @Override
    public double getCoins(String name) {
        double coins = -1;
        try (Jedis jedis = pool.getResource()) {
            if (CoinsAPI.isindb(name)) {
                coins = Double.valueOf(jedis.hget("coins_data", core.getUUID(name, false).toString()));
            } else if (core.isOnline(name)) {
                coins = core.getConfig().getDouble("General.Starting Coins", 0);
                createPlayer(core.getUUID(name, false), name, coins);
            }
        }
        return coins;
    }

    @Override
    public CoinsResponse setCoins(UUID uuid, double amount) {
        CoinsResponse response;
        try (Jedis jedis = pool.getResource()) {
            jedis.hset("coins_data", uuid.toString(), Double.toString(amount));
            jedis.publish("coins-data-update", uuid + " " + amount);
            response = new CoinsResponse(CoinsResponseType.SUCCESS, "");
        } catch (Exception ex) {
            response = new CoinsResponse(CoinsResponseType.FAILED, "Unknown database error");
            core.log("An error has ocurred in the database setting coins for '" + uuid + "', check the logs for more info.");
            core.debug("Redis database error message: " + ex.getMessage());
        }
        return response;
    }

    @Override
    public CoinsResponse setCoins(String name, double amount) {
        CoinsResponse response;
        try (Jedis jedis = pool.getResource()) {
            jedis.hset("coins_data", core.getUUID(name, false).toString(), Double.toString(amount));
            jedis.publish("coins-data-update", core.getUUID(name, false) + " " + amount);
            response = new CoinsResponse(CoinsResponseType.SUCCESS, "");
        } catch (Exception ex) {
            response = new CoinsResponse(CoinsResponseType.FAILED, "Unknown database error");
            core.log("An error has ocurred in the database setting coins for '" + name + "', check the logs for more info.");
            core.debug("Redis database error message: " + ex.getMessage());
        }
        return response;
    }

    @Override
    public boolean isindb(UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hexists("coins_nicks", uuid.toString());
        }
    }

    @Override
    public boolean isindb(String name) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hexists("coins_uuids", name.toLowerCase());
        }
    }

    @Override
    public void createPlayer(UUID uuid, String name, double balance) {
        Validate.notNull(uuid, "Can't create a player with null UUID");
        Validate.notNull(name, "Can't create a player with null name");
        if (CoinsAPI.isindb(uuid)) {
            return;
        }
        try (Jedis jedis = pool.getResource()) {
            core.debug("Creating data for player: " + name + " in the database.");
            jedis.hset("coins_data", uuid.toString(), Double.toString(balance));
            jedis.hset("coins_logins", uuid.toString(), Long.toString(System.currentTimeMillis()));
            jedis.hset("coins_nicks", uuid.toString(), name.toLowerCase());
            jedis.hset("coins_uuids", name.toLowerCase(), uuid.toString());
            jedis.publish("coins-data-update", uuid + " " + balance);
        } catch (Exception ex) {
            core.log("An error has ocurred creating a player in the redis database");
            core.debug(ex.getMessage());
        }
    }

    @Override
    public void updatePlayer(UUID uuid, String name) {
        if (core.getConfig().isOnline() && CoinsAPI.isindb(uuid)) {
            try (Jedis jedis = pool.getResource()) {
                String oldname = getNick(uuid);
                if (!oldname.equals(name.toLowerCase())) {
                    jedis.hdel("coins_uuids", oldname);
                    jedis.hset("coins_uuids", name.toLowerCase(), uuid.toString());
                }
                jedis.hset("coins_logins", uuid.toString(), Long.toString(System.currentTimeMillis()));
                jedis.hset("coins_nicks", uuid.toString(), name.toLowerCase());
                core.debug("Updated the name for '" + uuid + "' (" + name + ")");
            }
        } else if (!core.getConfig().isOnline() && CoinsAPI.isindb(name)) {
            try (Jedis jedis = pool.getResource()) {
                UUID olduuid = getUUID(name);
                if (!olduuid.equals(uuid)) {
                    jedis.hdel("coins_nicks", olduuid.toString());
                    jedis.hdel("coins_logins", olduuid.toString());
                    jedis.hset("coins_nicks", uuid.toString(), name.toLowerCase());
                    jedis.hset("coins_uuids", name.toLowerCase(), uuid.toString());
                    jedis.hset("coins_data", uuid.toString(), jedis.hget("coins_data", olduuid.toString()));
                }
                jedis.hset("coins_logins", uuid.toString(), Long.toString(System.currentTimeMillis()));
                core.debug("Updated the UUID for '" + name + "' (" + uuid + ")");
            }
        } else {
            core.debug("Tried to update a player that isn't in the database.");
        }
    }

    @Override
    public Map<String, Double> getTopPlayers(int top) {
        Map<String, Double> topMap = new HashMap<>();
        try (Jedis jedis = pool.getResource()) {
            for (int i = 0; i < top; i++) {
                DatabaseUtils.sortByValue(jedis.hgetAll("coins_data")).entrySet().forEach(ent -> topMap.put(ent.getKey(), Double.valueOf(ent.getValue())));
            }
        }
        return DatabaseUtils.sortByValue(topMap);
    }

    @Override
    public String getNick(UUID uuid) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hget("coins_nicks", uuid.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public UUID getUUID(String name) {
        try (Jedis jedis = pool.getResource()) {
            return UUID.fromString(jedis.hget("coins_uuids", name.toLowerCase()));
        } catch (Exception ex) {
            return null;
        }
    }

    public int getLastMultiplierID() {
        try (Jedis jedis = pool.getResource()) {
            return Integer.parseInt(jedis.get("coins_lastmultiplierid"));
        }
    }

    @Override
    public void createMultiplier(UUID uuid, int amount, int minutes, String server, MultiplierType type) {
        try (Jedis jedis = pool.getResource()) {
            Multiplier multiplier = MultiplierBuilder.newBuilder().setServer(server).setType(type).setData(new MultiplierData(amount, minutes)).setEnablerUUID(uuid).setEnablerName(core.getNick(uuid, false)).setID(getLastMultiplierID()).setAmount(amount).setMinutes(minutes).build();
            jedis.hset("coins_multipliers", uuid.toString(), jedis.hget("coins_multipliers", uuid.toString()) != null ? jedis.hget("coins_multipliers", uuid.toString()) + "," : "" + Integer.toString(multiplier.getId()));
            jedis.set("coins_multiplier:" + multiplier.getId(), multiplier.toString());
            jedis.incr("coins_lastmultiplierid");
        }
    }

    @Override
    public void deleteMultiplier(Multiplier multiplier) {
        try (Jedis jedis = pool.getResource()) {
            String multipliers = jedis.hget("coins_multipliers", multiplier.getEnablerUUID().toString()) != null ? jedis.hget("coins_multipliers", multiplier.getEnablerUUID().toString()) : "";
            if (multipliers.contains(Integer.toString(multiplier.getId()))) {
                multipliers = multipliers.replace(Integer.toString(multiplier.getId()), "");
                multipliers = multipliers.replace(",,", ",");
            }
            jedis.hset("coins_multipliers", multiplier.getEnablerUUID().toString(), multipliers);
            jedis.del("coins_multiplier:" + multiplier.getId());
            jedis.publish("coins-multiplier-disable", multiplier.toString());
        } catch (Exception ex) {
            core.log("An error has ocurred deleting the multiplier #" + multiplier.getId());
            core.debug(ex.getMessage());
        }
    }

    @Override
    public void enableMultiplier(Multiplier multiplier) {
        try (Jedis jedis = pool.getResource()) {
            jedis.set("coins_multiplier:" + multiplier.getId(), multiplier.toString());
            jedis.publish("coins-multiplier", multiplier.toString());
            jedis.publish("coins-event", "{\"event\":\"MultiplierEnableEvent\",\"multiplier\":\"" + multiplier.toJson() + "\"}");
        } catch (Exception ex) {
            core.log("An error has ocurred enabling the multiplier #" + multiplier.getId());
            core.debug(ex.getMessage());
        }
    }

    @Override
    public Set<Multiplier> getMultipliers(UUID uuid, boolean server) {
        Set<Multiplier> multipliers = new HashSet<>();
        try (Jedis jedis = pool.getResource()) {
            String multiplier = jedis.hget("coins_multipliers", uuid.toString()) != null ? jedis.hget("coins_multipliers", uuid.toString()) : "";
            String[] multiplier_ids = multiplier.split(",");
            if (multiplier_ids.length > 0) {
                for (int i = 0; i < multiplier_ids.length; i++) {
                    multipliers.add(getMultiplier(i));
                }
            }
        }
        return multipliers;
    }

    @Override
    public Multiplier getMultiplier(int id) {
        try (Jedis jedis = pool.getResource()) {
            if (jedis.exists("coins_multiplier:" + id)) {
                return Multiplier.fromJson(jedis.get("coins_multiplier:" + id));
            } else {
                return null;
            }
        }
    }

    @Override
    public Map<String, Double> getAllPlayers() {
        Map<String, Double> players = new HashMap<>();
        try (Jedis jedis = pool.getResource()) {
            Map<String, String> data = jedis.hgetAll("coins_data");
            data.entrySet().forEach(ent -> {
                players.put(ent.getKey() + "," + core.getNick(UUID.fromString(ent.getKey()), false), Double.valueOf(ent.getValue()));
            });
        } catch (Exception ex) {
            core.log("An error has ocurred getting all the players from the database, check the logs for more info.");
            core.debug(ex.getMessage());
        }
        return players;
    }

    @Override
    public void shutdown() {
        psl.poison();
        pool.close();
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    class PubSubListener implements Runnable {

        private JedisPubSub jpsh = new JedisPubSubHandler();

        @Override
        public void run() {
            boolean broken = false;
            try (Jedis rsc = pool.getResource()) {
                try {
                    jpsh = new JedisPubSubHandler();
                    rsc.subscribe(jpsh, "coins-data-update", "coins-multiplier", "coins-multiplier-disable", "coins-event");
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

    private class JedisPubSubHandler extends JedisPubSub {

        @Override
        public void onMessage(String channel, String message) {
            core.debug("Redis Log: Recived a message in channel: " + channel);
            core.debug("Redis Log: Message is:");
            core.debug(message);
            switch (channel) {
                case "coins-data-update":
                    CacheManager.updateCoins(UUID.fromString(message.split(" ")[0]), Double.valueOf(message.split(" ")[1]));
                    break;
                case "coins-multiplier":
                    Multiplier multiplier = Multiplier.fromJson(message);
                    CacheManager.addMultiplier(multiplier.getServer(), multiplier);
                    break;
                case "coins-multiplier-disable":
                    Multiplier.fromJson(message).disable();
                    break;
                case "coins-event":
                    JsonObject event = core.getGson().fromJson(message, JsonObject.class);
                    switch (event.get("event").getAsString()) {
                        case "MultiplierEnableEvent":
                            core.getMethods().callMultiplierEnableEvent(Multiplier.fromJson(event.get("multiplier").getAsString()));
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
