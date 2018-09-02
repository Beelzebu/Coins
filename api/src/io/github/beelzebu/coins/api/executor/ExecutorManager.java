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
package io.github.beelzebu.coins.api.executor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Beelzebu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExecutorManager {

    private static final Set<Executor> EXECUTORS = Collections.synchronizedSet(new LinkedHashSet<>());

    public static void addExecutor(Executor ex) {
        synchronized (EXECUTORS) {
            if (EXECUTORS.stream().noneMatch(executor -> ex.getId().equals(executor.getId()))) {
                EXECUTORS.add(ex);
            }
        }
    }

    public static Set<Executor> getExecutors() {
        return EXECUTORS;
    }

    public static Executor getExecutor(String id) {
        for (Executor ex : EXECUTORS) {
            if (ex.getId().equals(id)) {
                return ex;
            }
        }
        return null;
    }
}
