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

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.nifheim.beelzebu.coins.CoinsResponse;
import net.nifheim.beelzebu.coins.core.multiplier.Multiplier;
import net.nifheim.beelzebu.coins.core.multiplier.MultiplierType;

/**
 *
 * @author Beelzebu
 */
public class SQLite implements CoinsDatabase {

    @Override
    public void setup() {
        throw new UnsupportedOperationException("setup is not finished yet.");
    }

    @Override
    public double getCoins(UUID uuid) {
        throw new UnsupportedOperationException("getCoins is not finished yet.");
    }

    @Override
    public double getCoins(String name) {
        throw new UnsupportedOperationException("getCoins is not finished yet.");
    }

    @Override
    public CoinsResponse setCoins(UUID uuid, double amount) {
        throw new UnsupportedOperationException("setCoins is not finished yet.");
    }

    @Override
    public CoinsResponse setCoins(String name, double amount) {
        throw new UnsupportedOperationException("setCoins is not finished yet.");
    }

    @Override
    public boolean isindb(UUID uuid) {
        throw new UnsupportedOperationException("isindb is not finished yet.");
    }

    @Override
    public boolean isindb(String name) {
        throw new UnsupportedOperationException("isindb is not finished yet.");
    }

    @Override
    public void createPlayer(UUID uuid, String name, double balance) {
        throw new UnsupportedOperationException("createPlayer is not finished yet.");
    }

    @Override
    public void updatePlayer(UUID uuid, String name) {
        throw new UnsupportedOperationException("updatePlayer is not finished yet.");
    }

    @Override
    public Map<String, Double> getTopPlayers(int top) {
        throw new UnsupportedOperationException("getTopPlayers is not finished yet.");
    }

    @Override
    public String getNick(UUID uuid) {
        throw new UnsupportedOperationException("getNick is not finished yet.");
    }

    @Override
    public UUID getUUID(String name) {
        throw new UnsupportedOperationException("getUUID is not finished yet.");
    }

    @Override
    public void createMultiplier(UUID uuid, int amount, int minutes, String server, MultiplierType type) {
        throw new UnsupportedOperationException("createMultiplier is not finished yet.");
    }

    @Override
    public void deleteMultiplier(Multiplier multiplier) {
        throw new UnsupportedOperationException("deleteMultiplier is not finished yet.");
    }

    @Override
    public void enableMultiplier(Multiplier multiplier) {
        throw new UnsupportedOperationException("enableMultiplier is not finished yet.");
    }

    @Override
    public Set<Multiplier> getMultipliers(UUID uuid, boolean server) {
        throw new UnsupportedOperationException("getMultipliers is not finished yet.");
    }

    @Override
    public Multiplier getMultiplier(int id) {
        throw new UnsupportedOperationException("getMultiplier is not finished yet.");
    }

    @Override
    public Map<String, Double> getAllPlayers() {
        throw new UnsupportedOperationException("getAllPlayers is not finished yet.");
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException("shutdown is not finished yet.");
    }
}
