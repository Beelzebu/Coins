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
package io.github.beelzebu.coins.api.storage;

import io.github.beelzebu.coins.api.CoinsResponse;
import io.github.beelzebu.coins.api.CoinsTopEntry;
import io.github.beelzebu.coins.api.Multiplier;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * @author Beelzebu
 */
public interface StorageProvider {

    void setup();

    void shutdown();

    void createPlayer(@Nonnull UUID uuid, @Nonnull String name, double balance);

    void updatePlayer(@Nonnull UUID uuid, @Nonnull String name);

    UUID getUUID(String name);

    String getName(UUID uuid);

    double getCoins(UUID uuid);

    CoinsResponse setCoins(UUID uuid, double balance);

    boolean isindb(UUID uuid);

    boolean isindb(String name);

    LinkedHashSet<CoinsTopEntry> getTopPlayers(int top);

    void saveMultiplier(Multiplier multiplier);

    Multiplier getMultiplier(int id);

    Set<Multiplier> getMultipliers(UUID uuid);

    Set<Multiplier> getMultipliers(UUID uuid, String server);

    Set<Multiplier> getMultipliers();

    void enableMultiplier(Multiplier multiplier);

    void deleteMultiplier(Multiplier multiplier);

    LinkedHashMap<String, Double> getAllPlayers();

}
