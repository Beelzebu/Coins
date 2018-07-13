/*
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
package io.github.beelzebu.coins.api.cache;

import io.github.beelzebu.coins.api.Multiplier;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * @author Beelzebu
 */
public interface CacheProvider {

    /**
     * Get the coins of this player from the cache.
     *
     * @param uuid player to lookup in the cache.
     * @return optional wich may or may not contain the coins.
     */
    Optional<Double> getCoins(UUID uuid);

    void updatePlayer(UUID uuid, double coins);

    void removePlayer(UUID uuid);

    Optional<Multiplier> getMultiplier(int id);

    Set<Multiplier> getMultipliers(String server);

    void addMultiplier(Multiplier multiplier);

    /**
     * Remove a multiplier from the cache and enabled multipliers storage
     * (multipliers.json file in plugin's data folder)
     *
     * @param multiplier what multiplier we should delete.
     */
    void deleteMultiplier(Multiplier multiplier);

    void updateMultiplier(Multiplier multiplier, boolean callenable);

    void addQueueMultiplier(Multiplier multiplier);

    void removeQueueMultiplier(Multiplier multiplier);

    Set<Multiplier> getMultipliers();

    Set<UUID> getPlayers();

}
