/**
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
package io.github.beelzebu.coins.common.utils.database;

import io.github.beelzebu.coins.common.database.CoinsDatabase;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 * @author Beelzebu
 */
@AllArgsConstructor
public enum SQLQuery {
    /**
     * Select an user by his uuid.
     *
     * @param uuid UUID for the query.
     */
    SEARCH_USER_ONLINE("SELECT * FROM `" + CoinsDatabase.DATA_TABLE + "` WHERE uuid = ?;"),
    /**
     * Select an user by his name.
     *
     * @param name Username for the query.
     */
    SEARCH_USER_OFFLINE("SELECT * FROM `" + CoinsDatabase.DATA_TABLE + "` WHERE nick = ?;"),
    /**
     * Update coins for a user by his uuid:
     *
     * @param balance New balance to set.
     * @param uuid UUID for the query.
     */
    UPDATE_COINS_ONLINE("UPDATE `" + CoinsDatabase.DATA_TABLE + "` SET balance = ? WHERE uuid = ?;"),
    /**
     * Update data for a user when the server is in online mode.
     *
     * @param name Username to update.
     * @param lastlogin Lastlogin in millis.
     * @param uuid UUID for the query.
     */
    UPDATE_USER_ONLINE("UPDATE `" + CoinsDatabase.DATA_TABLE + "` SET nick = ?, lastlogin = ? WHERE uuid = ?;"),
    /**
     * Update data for a user when the server is in online mode.
     *
     * @param uuid UUID to update.
     * @param lastlogin Lastlogin in millis.
     * @param name Username for the query.
     */
    UPDATE_USER_OFFLINE("UPDATE `" + CoinsDatabase.DATA_TABLE + "` SET uuid = ?, lastlogin = ? WHERE nick = ?;"),
    /**
     * Create a user in the database.
     *
     * @param uuid UUID of the user.
     * @param name Username of the user.
     * @param balance Starting coins.
     * @param lastlogin Current time in millis.
     */
    CREATE_USER("INSERT INTO `" + CoinsDatabase.DATA_TABLE + "` (`id`, `uuid`, `nick`, `balance`, `lastlogin`) VALUES (null, ?, ?, ?, ?);"),
    /**
     * Create a multiplier in the database.
     *
     * @param server Servername for the multiplier.
     * @param uuid UUID of the owner of the multiplier.
     * @param multipliertype Type of this multiplier.
     * @param amount Amount of this multiplier.
     * @param minutes Minutes that the multiplier must be enabled.
     * @param queue If the multiplier must be added to the queue.
     * @param enabled If the multiplier is enabled.
     *
     * @see io.github.beelzebu.coins.MultiplierType
     */
    CREATE_MULTIPLIER("INSERT INTO `" + CoinsDatabase.DATA_TABLE + "multipliers` (`id`, `server`, `uuid`, `type`, `amount`, `minutes`, `endtime`, `queue`, `enabled`) VALUES (null, ?, ?, ?, ?, ?, ?, ?, ?);"),
    /**
     * Select top users from the database.
     *
     * @param limit Limit of users to select.
     */
    SELECT_TOP("SELECT * FROM `" + CoinsDatabase.DATA_TABLE + "` ORDER BY balance DESC LIMIT ?;"),
    DELETE_MULTIPLIER("DELETE FROM " + CoinsDatabase.MULTIPLIERS_TABLE + " WHERE id = ?;"),
    ENABLE_MULTIPLIER("UPDATE " + CoinsDatabase.MULTIPLIERS_TABLE + " SET enabled = true WHERE id = ;"),
    SELECT_ALL_MULTIPLIERS("SELECT * FROM " + CoinsDatabase.MULTIPLIERS_TABLE + " WHERE uuid = ? AND enabled = false AND queue = false;"),
    SELECT_ALL_MULTIPLIERS_SERVER("SELECT * FROM " + CoinsDatabase.MULTIPLIERS_TABLE + " WHERE uuid = ? AND enabled = false AND queue = false AND server = ?;"),
    SELECT_MULTIPLIER("SELECT * FROM " + CoinsDatabase.MULTIPLIERS_TABLE + " WHERE id = ?;"),
    SELECT_ALL_PLAYERS("SELECT * FROM " + CoinsDatabase.DATA_TABLE + ";");

    @Getter
    final String query;

}
