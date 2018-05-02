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
package io.github.beelzebu.coins.common;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.common.database.StorageType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Beelzebu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CacheManager {

    private static final CoinsCore CORE = CoinsCore.getInstance();
    public static final File MULTIPLIERS_FILE = new File(CORE.getBootstrap().getDataFolder(), "multipliers.json");
    @Getter
    private static final LoadingCache<UUID, Double> playersData = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<UUID, Double>() {
        @Override
        public Double load(UUID key) {
            CORE.getDatabase().updatePlayer(key, CORE.getNick(key, false).toLowerCase());
            return CORE.getDatabase().getCoins(key);
        }
    });
    @Getter
    private static final LoadingCache<String, Multiplier> multipliersData = Caffeine.newBuilder().build(new CacheLoader<String, Multiplier>() {
        @Override
        public Multiplier load(String key) {
            Iterator<Multiplier> mult = queuedMultipliers.iterator();
            if (mult.hasNext()) {
                Multiplier multi = mult.next();
                CORE.getMessagingService().enableMultiplier(multi);
                multi.enable(multi.getEnablerUUID(), multi.getEnablerName(), false);
                mult.remove();
                return multi;
            }
            return null;
        }
    });
    @Getter
    private static final List<Multiplier> queuedMultipliers = new ArrayList<>();

    public static double getCoins(UUID uuid) {
        if (uuid != null) {
            return playersData.get(uuid);
        }
        return -1;
    }

    /**
     * Update coins in this cache.
     *
     * @param uuid player to update.
     * @param coins coins to set.
     */
    public static void updateCoins(UUID uuid, double coins) {
        if (coins > -1) {
            if (CORE.getStorageType().equals(StorageType.SQLITE)) {
                CORE.getDatabase().setCoins(uuid, coins);
            }
            playersData.put(uuid, coins);
            CORE.debug("Updated local data for: " + uuid);
        }
    }

    /**
     * Publish userdata to all servers to update the cache.
     *
     * @param uuid player to publish.
     * @param coins coins to publish.
     */
    public static void publishUserdata(UUID uuid, double coins) {
        Preconditions.checkNotNull(uuid, "UUID can't be null");
        CORE.getMessagingService().publishUser(uuid, coins);
    }

    public static void addMultiplier(String server, Multiplier multiplier) {
        multiplier.setServer(multiplier.getServer().toLowerCase());
        multipliersData.put(server, multiplier);
        try {
            if (!MULTIPLIERS_FILE.exists()) {
                MULTIPLIERS_FILE.createNewFile();
            }
            Iterator<String> lines = FileUtils.readLines(MULTIPLIERS_FILE, Charsets.UTF_8).iterator();
            boolean exists = false;
            while (lines.hasNext()) {
                String line = lines.next();
                Multiplier mult = Multiplier.fromJson(line, false);
                if (mult.getId() == multiplier.getId()) {
                    exists = true;
                    CORE.debug("Trying to add an existent multiplier: " + mult.toJson());
                    break;
                }
            }
            if (!exists) {
                CORE.getMessagingService().enableMultiplier(multiplier);
                try {
                    FileUtils.writeLines(MULTIPLIERS_FILE, Collections.singletonList(multiplier.toJson().toString() + "\n"), true);
                } catch (IOException ex) {
                    CORE.log("An error has ocurred saving a multiplier in the local storage.");
                    CORE.debug(ex.getMessage());
                }
            }
        } catch (IOException ex) {
            CORE.log("An error has ocurred saving a multiplier in the local storage.");
            CORE.debug(ex.getMessage());
        }
    }

    public static Multiplier getMultiplier(String server) {
        Multiplier multiplier = multipliersData.getIfPresent(server.replaceAll(" ", "").toLowerCase());
        if (multiplier == null) {
            for (String sv : multipliersData.asMap().keySet()) {
                if (sv.split(" ")[0].toLowerCase().equals(server.replaceAll(" ", "").toLowerCase())) {
                    multiplier = multipliersData.getIfPresent(sv);
                    break;
                }
            }
        }
        if (multiplier != null) {
            multiplier.checkMultiplierTime();
        }
        return multiplier;
    }

    public static void deleteMultiplier(Multiplier multplier) {
        if (multplier == null) {
            return;
        }
        try {
            Iterator<String> lines = FileUtils.readLines(MULTIPLIERS_FILE, Charsets.UTF_8).iterator();
            while (lines.hasNext()) {
                Multiplier multiplier = Multiplier.fromJson(lines.next(), false);
                if (multiplier.getId() == multplier.getId()) {
                    multipliersData.invalidate(multiplier.getServer());
                    lines.remove();
                    break;
                }
            }
            FileUtils.writeLines(MULTIPLIERS_FILE, Lists.newArrayList(lines));
        } catch (IOException ex) {
            CORE.log("An error has ocurred removing a multiplier from local storage.");
            CORE.debug(ex.getMessage());
        }
        multipliersData.invalidate(multplier.getServer());
    }

    public static void updateMultiplier(Multiplier multiplier, boolean callenable) {
        Preconditions.checkNotNull(multiplier, "Multiplier can't be null");
        if (callenable) {
            multiplier.enable(multiplier.getEnablerUUID(), multiplier.getEnablerName(), true);
            CORE.getMessagingService().enableMultiplier(multiplier);
        } else {
            CORE.getMessagingService().publishMultiplier(multiplier);
        }
    }
}
