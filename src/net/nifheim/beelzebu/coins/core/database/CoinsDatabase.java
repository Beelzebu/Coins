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
import net.nifheim.beelzebu.coins.core.Core;
import net.nifheim.beelzebu.coins.core.multiplier.Multiplier;
import net.nifheim.beelzebu.coins.core.multiplier.MultiplierType;

/**
 *
 * @author Beelzebu
 */
public interface CoinsDatabase {

    public final Core core = Core.getInstance();
    public static final String prefix = core.getConfig().getString("MySQL.Prefix");

    void setup();

    double getCoins(UUID uuid);

    double getCoins(String name);

    CoinsResponse setCoins(UUID uuid, double amount);

    CoinsResponse setCoins(String name, double amount);
    
    boolean isindb(UUID uuid);
    
    boolean isindb(String name);

    void createPlayer(UUID uuid, String name, double balance);
    
    void updatePlayer(UUID uuid, String name);
    
    Map<String, Double> getTopPlayers(int top);
    
    String getNick(UUID uuid);
    
    UUID getUUID(String name);
    
    void createMultiplier(UUID uuid, int amount, int minutes, String server, MultiplierType type);
    
    void deleteMultiplier(Multiplier multiplier);
    
    void enableMultiplier(Multiplier multiplier);
    
    Set<Multiplier> getMultipliers(UUID uuid, boolean server);
    
    Multiplier getMultiplier(int id);
    
    Map<String, Double> getAllPlayers();
    
    void shutdown();
}
