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
package net.nifheim.beelzebu.coins.core;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import net.nifheim.beelzebu.coins.core.multiplier.Multiplier;

/**
 *
 * @author Beelzebu
 */
public class CacheManager {

    private static final Core core = Core.getInstance();
    @Getter
    private static final LoadingCache<UUID, Double> playersData = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<UUID, Double>() {
        @Override
        public Double load(UUID key) {
            double coins = core.getDatabase().getCoins(key);
            core.getDatabase().updatePlayer(key, core.getNick(key, false).toLowerCase());
            core.debug("Loaded " + key + " into cache.");
            return coins;
        }
    });
    @Getter
    private static final LoadingCache<String, Multiplier> multipliersData = CacheBuilder.newBuilder().build(new CacheLoader<String, Multiplier>() {
        @Override
        public Multiplier load(String key) {
            for (Multiplier mult : queuedMultipliers) {
                if (mult.getServer().equals(key)) {
                    return mult;
                }
            }
            return null;
        }
    });
    @Getter
    private static final List<Multiplier> queuedMultipliers = new ArrayList<>();

    public static double getCoins(UUID uuid) {
        if (uuid != null) {
            return playersData.getUnchecked(uuid);
        }
        return -1;
    }

    public static void updateCoins(UUID uuid, double coins) {
        if (coins > -1) {
            core.debug("Updated " + uuid + " in cache.");
            playersData.put(uuid, coins);
        }
    }

    public static void addMultiplier(String server, Multiplier multiplier) {
        multipliersData.put(server, multiplier);
    }

    public static void removeMultiplier(String server) {
        multipliersData.invalidate(server);
    }

    public static Multiplier getMultiplier(String server) {
        return multipliersData.getIfPresent(server);
    }
}
