/**
 * This file is part of Coins
 *
 * Copyright (C) 2017 Beelzebu
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package net.nifheim.beelzebu.coins.common.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import net.nifheim.beelzebu.coins.common.CoinsCore;

/**
 * @author Beelzebu
 */
public interface Database {

    String prefix = CoinsCore.getInstance().isMySQL() ? CoinsCore.getInstance().getConfig().getString("MySQL.Prefix") : "";

    Double getCoins(String player);

    void addCoins(String player, Double coins);

    void takeCoins(String player, Double coins);

    void resetCoins(String player);

    void setCoins(String player, Double coins);

    boolean isindb(String player);

    Double getCoins(UUID player);

    void addCoins(UUID player, Double coins);

    void takeCoins(UUID player, Double coins);

    void resetCoins(UUID player);

    void setCoins(UUID player, Double coins);

    boolean isindb(UUID player);

    @Deprecated
    List<String> getTop(int top);

    Map<String, Double> getTopPlayers(int top);

    void createPlayer(Connection c, String player, UUID uuid, double balance);

    Connection getConnection() throws SQLException;

    String getNick(UUID uuid);

    UUID getUUID(String nick);

    Map<String, Double> getAllPlayers();

    void shutdown();

    enum SQLQuery {
        SEARCH_USER_ONLINE("SELECT * FROM `" + Database.prefix + "Data` WHERE uuid = ?;"),
        SEARCH_USER_OFFLINE("SELECT * FROM `" + Database.prefix + "Data` WHERE nick = ?;"),
        CREATE_USER("INSERT INTO `" + Database.prefix + "Data` (`uuid`, `nick`, `balance`, `lastlogin`) VALUES (?, ?, ?, ?);"),
        UPDATE_USER_ONLINE("UPDATE `" + Database.prefix + "Data` SET nick = ?, lastlogin = ? WHERE uuid = ?;"),
        UPDATE_USER_OFFLINE("UPDATE `" + Database.prefix + "Data` SET uuid = ?, lastlogin = ? WHERE nick = ?;"),
        UPDATE_COINS_ONLINE("UPDATE `" + Database.prefix + "Data` SET balance = ? WHERE uuid = ?;"),
        UPDATE_COINS_OFFLINE("UPDATE `" + Database.prefix + "Data` SET balance = ? WHERE nick = ?;"),
        SELECT_TOP("SELECT * FROM `" + Database.prefix + "Data` ORDER BY balance DESC LIMIT ?;");

        private final String name;

        SQLQuery(String query) {
            name = query;
        }
    }

    class Utils {

        static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
            return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Collections.reverseOrder())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        }

        public static PreparedStatement generatePreparedStatement(Connection c, SQLQuery query, Object... parameters) throws SQLException {
            PreparedStatement ps = c.prepareStatement(query.name);
            try {
                if (parameters.length > 0) {
                    for (int i = 1; i <= parameters.length; i++) {
                        Object parameter = parameters[i - 1];
                        if (parameter == null) {
                            throw new IllegalArgumentException("The parameter can't be null.");
                        }
                        if (parameter instanceof String) {
                            ps.setString(i, parameter.toString());
                        } else if (parameter instanceof UUID) {
                            ps.setString(i, parameter.toString());
                        } else if (parameter instanceof Integer) {
                            ps.setInt(i, (int) parameter);
                        } else if (parameter instanceof Long) {
                            ps.setLong(i, (long) parameter);
                        } else if (parameter instanceof Double) {
                            ps.setDouble(i, (double) parameter);
                        } else {
                            CoinsCore.getInstance().debug("We can't put the object '" + parameter.toString() + "' in the query.");
                        }
                    }
                }
            } catch (SQLException ex) {
                CoinsCore.getInstance().log("An internal error has ocurred while trying to execute a query in the database, check the logs to get more information.");
                CoinsCore.getInstance().debug("The error code is: '" + ex.getErrorCode() + "'");
                CoinsCore.getInstance().debug("The error message is: '" + ex.getMessage() + "'");
                CoinsCore.getInstance().debug("Query: " + query.name);
            }
            return ps;
        }
    }
}
