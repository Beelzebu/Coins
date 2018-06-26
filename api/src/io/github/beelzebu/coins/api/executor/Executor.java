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
package io.github.beelzebu.coins.api.executor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 * @author Beelzebu
 */
@Getter
@AllArgsConstructor
public class Executor {

    private final String id;
    private final String displayname;
    private final double cost;
    private final List<String> commands;

    public JsonObject toJson() {
        return toJson(this);
    }

    public static JsonObject toJson(Executor ex) {
        return CoinsPlugin.getInstance().getGson().toJsonTree(ex).getAsJsonObject();
    }

    public static Executor fromJson(String json) throws JsonParseException {
        return CoinsPlugin.getInstance().getGson().fromJson(json, Executor.class);
    }
}
