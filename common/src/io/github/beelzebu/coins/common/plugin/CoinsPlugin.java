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
package io.github.beelzebu.coins.common.plugin;

import io.github.beelzebu.coins.common.dependency.DependencyManager;
import io.github.beelzebu.coins.common.executor.Executor;
import io.github.beelzebu.coins.common.executor.ExecutorManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 *
 * @author Beelzebu
 */
@RequiredArgsConstructor
public abstract class CoinsPlugin {

    @Getter
    protected final CoinsBootstrap bootstrap;
    @Getter
    private final DependencyManager dependencyManager = new DependencyManager();

    public void enable() {
        loadExecutors();
    }

    public void disable() {

    }

    public final void loadExecutors() {
        getBootstrap().getPluginConfig().getConfigurationSection("Command executor").forEach(id -> ExecutorManager.addExecutor(new Executor(id, getBootstrap().getPluginConfig().getString("Command executor." + id + ".Displayname", id), getBootstrap().getPluginConfig().getDouble("Command executor." + id + ".Cost", 0), getBootstrap().getPluginConfig().getStringList("Command executor." + id + ".Command"))));
    }
}
