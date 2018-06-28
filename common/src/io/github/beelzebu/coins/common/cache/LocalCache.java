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
package io.github.beelzebu.coins.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.api.Multiplier;
import io.github.beelzebu.coins.api.cache.CacheProvider;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Beelzebu
 */
public final class LocalCache implements CacheProvider {

    private final CoinsPlugin plugin = CoinsAPI.getPlugin();

    private final LoadingCache<UUID, Double> players = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build((UUID key) -> {
        plugin.getDatabase().updatePlayer(key, plugin.getName(key, false).toLowerCase());
        return plugin.getDatabase().getCoins(key);
    });
    private final List<Multiplier> queuedMultipliers = new ArrayList<>();
    private final LoadingCache<String, Multiplier> multipliers = Caffeine.newBuilder().build(k -> {
        Iterator<Multiplier> it = queuedMultipliers.iterator();
        if (it.hasNext()) {
            Multiplier multiplier = it.next();
            if (multiplier.getServer().equals(k)) {
                plugin.getMessagingService().enableMultiplier(multiplier);
                multiplier.enable(false);
                it.remove();
                return multiplier;
            }
        }
        return null;
    });

    @Override
    public double getCoins(UUID uuid) {
        if (uuid != null) {
            return players.get(uuid);
        }
        return -1;
    }

    @Override
    public void addPlayer(UUID uuid, double coins) {
        players.put(uuid, coins);
    }

    @Override
    public void removePlayer(UUID uuid) {
        players.invalidate(uuid);
    }

    @Override
    public Multiplier getMultiplier(String server) {
        Multiplier multiplier = multipliers.getIfPresent(server.replaceAll(" ", "").toLowerCase());
        if (multiplier == null) {
            for (String sv : multipliers.asMap().keySet()) {
                if (sv.split(" ")[0].toLowerCase().equals(server.replaceAll(" ", "").toLowerCase())) {
                    multiplier = multipliers.getIfPresent(sv);
                    break;
                }
            }
        }
        if (multiplier != null) {
            multiplier.checkMultiplierTime();
        }
        return multiplier;
    }

    @Override
    public void addMultiplier(String server, Multiplier multiplier) {
        multipliers.put(server, multiplier); // put the multiplier in the cache
        // store it in a local storage to load them again without quering database if the server is restarted
        try {
            if (!plugin.getMultipliersFile().exists()) {
                plugin.getMultipliersFile().createNewFile();
            }
            Iterator<String> lines = Files.readAllLines(plugin.getMultipliersFile().toPath()).iterator();
            boolean exists = false;
            // check if the multiplier was already stored in this server
            while (lines.hasNext()) {
                String line = lines.next();
                Multiplier mult = Multiplier.fromJson(line, false);
                if (mult.getId() == multiplier.getId()) {
                    exists = true;
                    plugin.debug("Trying to add an existent multiplier: " + mult.toJson());
                    break;
                }
            }
            if (!exists) {
                try {
                    Files.write(plugin.getMultipliersFile().toPath(), Collections.singletonList(multiplier.toJson().toString() + "\n"), StandardOpenOption.APPEND);
                } catch (IOException ex) {
                    plugin.log("An error has ocurred saving a multiplier in the local storage.");
                    plugin.debug(ex.getMessage());
                }
            }
        } catch (IOException ex) {
            plugin.log("An error has ocurred saving a multiplier in the local storage.");
            plugin.debug(ex.getMessage());
        }
    }

    @Override
    public void deleteMultiplier(Multiplier multiplier) {
        Preconditions.checkNotNull(multiplier, "Multiplier can't be null");
        try { // remove it from local multiplier storage
            Iterator<String> lines = Files.readAllLines(plugin.getMultipliersFile().toPath()).iterator();
            while (lines.hasNext()) {
                if (Multiplier.fromJson(lines.next(), false).getId() == multiplier.getId()) {
                    multipliers.invalidate(multiplier.getServer());
                    lines.remove();
                    break;
                }
            }
            Files.write(plugin.getMultipliersFile().toPath(), Lists.newArrayList(lines));
        } catch (IOException ex) {
            plugin.log("An error has ocurred removing a multiplier from local storage.");
            plugin.debug(ex.getMessage());
        }
        multipliers.invalidate(multiplier.getServer());
    }

    @Override
    public void updateMultiplier(Multiplier multiplier, boolean callenable) {
        Preconditions.checkNotNull(multiplier, "Multiplier can't be null");
        if (callenable) {
            multiplier.enable(true);
            plugin.getMessagingService().enableMultiplier(multiplier);
        } else {
            plugin.getMessagingService().updateMultiplier(multiplier);
        }
    }

    @Override
    public void addQueueMultiplier(Multiplier multiplier) {
        queuedMultipliers.add(multiplier);
    }

    @Override
    public void removeQueueMultiplier(Multiplier multiplier) {
        queuedMultipliers.remove(multiplier);
    }

    @Override
    public Set<Multiplier> getMultipliers() {
        return new HashSet<>(multipliers.asMap().values());
    }

    @Override
    public Map<UUID, Double> getPlayers() {
        return players.asMap();
    }
}
