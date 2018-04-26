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
package io.github.beelzebu.coins.common.utils.database;

import io.github.beelzebu.coins.common.CoinsCore;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 *
 * @author Beelzebu
 */
public class DatabaseUtils {

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        return map.entrySet().stream().sorted(Map.Entry.comparingByValue(Collections.reverseOrder())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public static PreparedStatement prepareStatement(Connection c, SQLQuery query, Object... parameters) throws SQLException {
        PreparedStatement ps = c.prepareStatement(query.getQuery());
        try {
            if (parameters.length > 0) {
                for (int i = 1; i <= parameters.length; i++) {
                    Object parameter = parameters[i - 1];
                    if (parameter == null) {
                        ps.setObject(i, null);
                    } else if (parameter instanceof String) {
                        ps.setString(i, parameter.toString().toLowerCase());
                    } else if (parameter instanceof UUID) {
                        ps.setString(i, parameter.toString());
                    } else if (parameter instanceof Integer) {
                        ps.setInt(i, (int) parameter);
                    } else if (parameter instanceof Long) {
                        ps.setLong(i, (long) parameter);
                    } else if (parameter instanceof Double) {
                        ps.setDouble(i, (double) parameter);
                    } else if (parameter instanceof Boolean) {
                        ps.setBoolean(i, (boolean) parameter);
                    } else {
                        ps.setString(i, parameter.toString());
                    }
                }
            }
        } catch (SQLException ex) {
            CoinsCore.getInstance().log("An internal error has ocurred while trying to execute a query in the database, check the logs to get more information.");
            CoinsCore.getInstance().debug("The error code is: '" + ex.getErrorCode() + "'");
            CoinsCore.getInstance().debug("The error message is: '" + ex.getMessage() + "'");
            CoinsCore.getInstance().debug("Query: " + query.getQuery());
        }
        return ps;
    }
}
