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
package io.github.beelzebu.coins.api.storage.sql;

import io.github.beelzebu.coins.api.CoinsAPI;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Beelzebu
 */
@AllArgsConstructor
public enum SQLQuery {
    /**
     * Select an user by his uuid.
     * <br></br>
     * <strong>Params:</strong>
     * <ul>
     * <li> UUID for the query. </li>
     * </ul>
     */
    SEARCH_USER_ONLINE("SELECT * FROM `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` WHERE uuid = ?;"),
    /**
     * Select an user by his name.
     * <br></br>
     * <strong>Params:</strong>
     * <ul>
     * <li> Username for the query</li>
     * </ul>
     */
    SEARCH_USER_OFFLINE("SELECT * FROM `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` WHERE nick = ?;"),
    /**
     * Update coins for a user by his uuid:
     * <br></br>
     * <strong>Params:</strong>
     * <ul>
     * <li> New balance to set.</li>
     * <li> UUID for the query</li>
     * </ul>
     */
    UPDATE_COINS_ONLINE("UPDATE `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` SET balance = ? WHERE uuid = ?;"),
    /**
     * Update data for a user when the server is in online mode.
     * <br></br>
     * <strong>Params:</strong>
     * <ul>
     * <li> Username to update.</li>
     * <li> Lastlogin in millis.</li>
     * <li> UUID for the query</li>
     * </ul>
     */
    UPDATE_USER_ONLINE("UPDATE `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` SET nick = ?, lastlogin = ? WHERE uuid = ?;"),
    /**
     * Update data for a user when the server is in online mode.
     * <br></br>
     * <strong>Params:</strong>
     * <ul>
     * <li> UUID to update.</li>
     * <li> Lastlogin in millis.</li>
     * <li> Username for the query</li>
     * </ul>
     */
    UPDATE_USER_OFFLINE("UPDATE `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` SET uuid = ?, lastlogin = ? WHERE nick = ?;"),
    /**
     * Create a user in the storageProvider.
     * <br></br>
     * <strong>Params:</strong>
     * <ul>
     * <li> UUID of the user.</li>
     * <li> Username of the user.</li>
     * <li> Starting coins.</li>
     * <li> Current time in millis</li>
     * </ul>
     */
    CREATE_USER("INSERT INTO `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` (`id`, `uuid`, `nick`, `balance`, `lastlogin`) VALUES (null, ?, ?, ?, ?);"),
    /**
     * Create a multiplier in the storageProvider.
     * <br></br>
     * <strong>Params:</strong>
     * <ul>
     * <li> Servername for the multiplier.</li>
     * <li> UUID of the owner of the multiplier.</li>
     * <li> Type of this multiplier.</li>
     * <li> Amount of this multiplier.</li>
     * <li> Minutes that the multiplier must be enabled.</li>
     * <li> If the multiplier must be added to the queue.</li>
     * <li> If the multiplier is enabled</li>
     * </ul>
     *
     * @see io.github.beelzebu.coins.api.MultiplierType
     */
    CREATE_MULTIPLIER("INSERT INTO `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getMultipliersTable() + "` (`id`, `server`, `uuid`, `type`, `amount`, `minutes`, `endtime`, `queue`, `enabled`) VALUES (null, ?, ?, ?, ?, ?, ?, ?, ?);"),
    /**
     * Select top users from the storageProvider.
     * <br></br>
     * <strong>Params:</strong>
     * <ul>
     * <li> Limit of users to select</li>
     * </ul>
     */
    SELECT_TOP("SELECT * FROM `" + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + "` ORDER BY balance DESC LIMIT ?;"),
    DELETE_MULTIPLIER("DELETE FROM " + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getMultipliersTable() + " WHERE id = ?;"),
    ENABLE_MULTIPLIER("UPDATE " + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getMultipliersTable() + " SET enabled = true WHERE id = ;"),
    SELECT_ALL_MULTIPLIERS("SELECT * FROM " + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getMultipliersTable()),
    SELECT_ALL_MULTIPLIERS_PLAYER("SELECT * FROM " + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getMultipliersTable() + " WHERE uuid = ? AND enabled = false AND queue = false;"),
    SELECT_ALL_MULTIPLIERS_SERVER("SELECT * FROM " + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getMultipliersTable() + " WHERE uuid = ? AND enabled = false AND queue = false AND server = ?;"),
    SELECT_MULTIPLIER("SELECT * FROM " + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getMultipliersTable() + " WHERE id = ?;"),
    SELECT_ALL_PLAYERS("SELECT * FROM " + ((SQLDatabase) CoinsAPI.getPlugin().getStorageProvider()).getDataTable() + ";");

    @Getter
    private final String query;

}
