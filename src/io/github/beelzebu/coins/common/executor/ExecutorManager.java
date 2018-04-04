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
package io.github.beelzebu.coins.common.executor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Beelzebu
 */
public class ExecutorManager {

    private final Set<Executor> executors = Collections.synchronizedSet(new HashSet<>());

    public synchronized void addExecutor(Executor ex) {
        executors.add(ex);
    }

    public synchronized Set<Executor> getExecutors() {
        return executors;
    }

    public synchronized Executor getExecutor(String id) {
        for (Executor ex : executors) {
            if (ex.getID().equals(id)) {
                return ex;
            }
        }
        return null;
    }
}
