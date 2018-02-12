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

import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import net.nifheim.beelzebu.coins.core.multiplier.Multiplier;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Beelzebu
 */
public class CacheManager {

    private static final Core core = Core.getInstance();
    private static final File multipliersdata = new File(core.getDataFolder(), "multipliers.json");
    @Getter
    private static final LoadingCache<UUID, Double> playersData = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<UUID, Double>() {
        @Override
        public Double load(UUID key) {
            core.getDatabase().updatePlayer(key, core.getNick(key, false).toLowerCase());
            core.debug("Loaded " + key + " into cache.");
            return core.getDatabase().getCoins(key);
        }
    });
    @Getter
    private static final LoadingCache<String, Multiplier> multipliersData = CacheBuilder.newBuilder().build(new CacheLoader<String, Multiplier>() {
        @Override
        public Multiplier load(String key) {
            for (Multiplier mult : queuedMultipliers) {
                if (mult.getServer().equals(key)) {
                    mult.enable(mult.getEnablerUUID(), mult.getEnablerName(), false);
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
        multiplier.setServer(multiplier.getServer().toLowerCase());
        multipliersData.put(server, multiplier);
        try {
            if (!multipliersdata.exists()) {
                multipliersdata.createNewFile();
            }
            Iterator<String> lines = FileUtils.readLines(multipliersdata, Charsets.UTF_8).iterator();
            while (lines.hasNext()) {
                String line = lines.next();
                Multiplier mult = Multiplier.fromJson(line);
                if (mult.getId() != multiplier.getId()) {
                    try {
                        FileUtils.writeLines(multipliersdata, Collections.singletonList(multiplier.toJson().toString()), true);
                    } catch (IOException ex) {
                        core.log("An error has ocurred saving a multiplier in the local storage.");
                        core.debug(ex.getMessage());
                    }
                }
            }
        } catch (IOException ex) {
            core.log("An error has ocurred saving a multiplier in the local storage.");
            core.debug(ex.getMessage());
        }
    }

    public static void removeMultiplier(String server) {
        try {
            Iterator<String> lines = FileUtils.readLines(multipliersdata, Charsets.UTF_8).iterator();
            while (lines.hasNext()) {
                Multiplier multiplier = Multiplier.fromJson(lines.next());
                if (multiplier.getServer().equals(server.toLowerCase())) {
                    lines.remove();
                }
            }
            FileUtils.writeLines(multipliersdata, Lists.newArrayList(lines));
        } catch (IOException ex) {
            core.log("An error has ocurred removing a multiplier from local storage.");
            core.debug(ex.getMessage());
        }
        multipliersData.invalidate(server);
    }

    public static Multiplier getMultiplier(String server) {
        Multiplier multiplier = multipliersData.getIfPresent(server);
        if (multiplier != null) {
            multiplier.checkMultiplierTime();
        }
        return multiplier;
    }
}
