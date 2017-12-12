/**
 * This file is part of Coins
 *
 * Copyright (C) 2017 Beelzebu
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
package net.nifheim.beelzebu.coins.database;

import java.util.Map;
import java.util.UUID;
import net.nifheim.beelzebu.coins.CoinsResponse;

/**
 *
 * @author Beelzebu
 */
public class NewRedis implements CoinsDatabase {

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

}
