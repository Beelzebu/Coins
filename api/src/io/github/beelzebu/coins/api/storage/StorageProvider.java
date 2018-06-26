/*
 * This file is part of coins-api
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
package io.github.beelzebu.coins.api.storage;

import io.github.beelzebu.coins.api.CoinsResponse;
import io.github.beelzebu.coins.api.Multiplier;
import io.github.beelzebu.coins.api.MultiplierType;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;

/**
 *
 * @author Beelzebu
 */
public interface StorageProvider {

    public void setup();

    public void shutdown();

    public void createPlayer(UUID uuid, String name, double balance);

    public void updatePlayer(UUID uuid, String name);

    public UUID getUUID(String name);

    public String getName(UUID uuid);

    public double getCoins(UUID uuid);

    public CoinsResponse setCoins(UUID uuid, double balance);

    public boolean isindb(UUID uuid);

    public boolean isindb(String name);

    public LinkedHashMap<String, Double> getTopPlayers(int top);

    public void createMultiplier(UUID uuid, int amount, int minutes, String server, MultiplierType type);

    public Multiplier getMultiplier(int id);

    public Set<Multiplier> getMultipliers(UUID uuid);

    public Set<Multiplier> getMultipliers(UUID uuid, String server);

    public void enableMultiplier(Multiplier multiplier);

    public void deleteMultiplier(Multiplier multiplier);

    public LinkedHashMap<String, Double> getAllPlayers();

}
