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
package io.github.beelzebu.coins.api.dependency.relocation;

import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Beelzebu
 */
@Getter
@AllArgsConstructor
public final class Relocation {

    private final String pattern;
    private final String relocatedPattern;

    public static Relocation of(String id, String pattern) {
        return new Relocation(pattern.replace("{}", "."), "io.github.beelzebu.lib." + id);
    }

    public static List<Relocation> allOf(Relocation... relocations) {
        return Arrays.asList(relocations);
    }
}
