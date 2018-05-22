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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import io.github.beelzebu.coins.Multiplier;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
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
    private static final LoadingCache<UUID, Double> PLAYERS_DATA = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build((UUID key) -> {
        CORE.getDatabase().updatePlayer(key, CORE.getNick(key, false).toLowerCase());
        return CORE.getDatabase().getCoins(key);
    });
    private static final LoadingCache<String, Multiplier> MULTIPLIERS_DATA = Caffeine.newBuilder().build(k -> {
        Iterator<Multiplier> it = CacheManager.QUEUED_MULTIPLIERS.iterator();
        if (it.hasNext()) {
            Multiplier multiplier = it.next();
            if (multiplier.getServer().equals(k)) {
                CORE.getMessagingService().enableMultiplier(multiplier);
                multiplier.enable(false);
                it.remove();
                return multiplier;
            }
        }
        return null;
    });
    private static final List<Multiplier> QUEUED_MULTIPLIERS = new ArrayList<>();

    public static LoadingCache<UUID, Double> getPlayersData() {
        return PLAYERS_DATA;
    }

    public static LoadingCache<String, Multiplier> getMultipliersData() {
        return MULTIPLIERS_DATA;
    }

    public static List<Multiplier> getQueuedMultipliers() {
        return QUEUED_MULTIPLIERS;
    }

    public static double getCoins(UUID uuid) {
        if (uuid != null) {
            return PLAYERS_DATA.get(uuid);
        }
        return -1;
    }

    public static void addMultiplier(String server, Multiplier multiplier) {
        MULTIPLIERS_DATA.put(server, multiplier); // put the multiplier in the cache
        // store it in a local storage to load them again without quering database if the server is restarted
        try {
            if (!MULTIPLIERS_FILE.exists()) {
                MULTIPLIERS_FILE.createNewFile();
            }
            Iterator<String> lines = FileUtils.readLines(MULTIPLIERS_FILE, Charsets.UTF_8).iterator();
            boolean exists = false;
            // check if the multiplier was already stored in this server
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
        Multiplier multiplier = MULTIPLIERS_DATA.getIfPresent(server.replaceAll(" ", "").toLowerCase());
        if (multiplier == null) {
            for (String sv : MULTIPLIERS_DATA.asMap().keySet()) {
                if (sv.split(" ")[0].toLowerCase().equals(server.replaceAll(" ", "").toLowerCase())) {
                    multiplier = MULTIPLIERS_DATA.getIfPresent(sv);
                    break;
                }
            }
        }
        if (multiplier != null) {
            multiplier.checkMultiplierTime();
        }
        return multiplier;
    }

    /**
     * Remove a multiplier from the cache and enabled multipliers storage
     * (multipliers.json file in plugin's data folder)
     *
     * @param multiplier what multiplier we should delete.
     */
    public static void deleteMultiplier(Multiplier multiplier) {
        Preconditions.checkNotNull(multiplier, "Multiplier can't be null");
        try { // remove it from local multiplier storage
            Iterator<String> lines = FileUtils.readLines(MULTIPLIERS_FILE, Charsets.UTF_8).iterator();
            while (lines.hasNext()) {
                if (Multiplier.fromJson(lines.next(), false).getId() == multiplier.getId()) {
                    MULTIPLIERS_DATA.invalidate(multiplier.getServer());
                    lines.remove();
                    break;
                }
            }
            FileUtils.writeLines(MULTIPLIERS_FILE, Lists.newArrayList(lines));
        } catch (IOException ex) {
            CORE.log("An error has ocurred removing a multiplier from local storage.");
            CORE.debug(ex.getMessage());
        }
        MULTIPLIERS_DATA.invalidate(multiplier.getServer());
    }

    public static void updateMultiplier(Multiplier multiplier, boolean callenable) {
        Preconditions.checkNotNull(multiplier, "Multiplier can't be null");
        if (callenable) {
            multiplier.enable(true);
            CORE.getMessagingService().enableMultiplier(multiplier);
        } else {
            CORE.getMessagingService().updateMultiplier(multiplier);
        }
    }
}
