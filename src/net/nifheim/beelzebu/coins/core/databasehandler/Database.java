/*
 * This file is part of Coins.
 *
 * Copyright © 2017 Beelzebu
 * Coins is licensed under the GNU General Public License.
 *
 * Coins is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Coins is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.nifheim.beelzebu.coins.core.databasehandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author Beelzebu
 */
public interface Database {

    Double getCoins(String player);

    void addCoins(String player, Double coins, Boolean multiply);

    void takeCoins(String player, Double coins);

    void resetCoins(String player);

    void setCoins(String player, Double coins);
    
    boolean isindb(String player);

    Double getCoins(UUID player);

    void addCoins(UUID player, Double coins, Boolean multiply);

    void takeCoins(UUID player, Double coins);

    void resetCoins(UUID player);

    void setCoins(UUID player, Double coins);
    
    boolean isindb(UUID player);
    
    List<String> getTop(int top);
    
    void createPlayer(String player, UUID uuid);
    
    Connection getConnection() throws SQLException;
}