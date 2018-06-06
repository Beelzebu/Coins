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
package io.github.beelzebu.coins.common.dependency;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.database.StorageType;
import io.github.beelzebu.coins.common.messaging.MessagingService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DependencyRegistry {

    private final CoinsCore core = CoinsCore.getInstance();
    private final Map<StorageType, List<Dependency>> STORAGE_DEPENDENCIES = ImmutableMap.<StorageType, List<Dependency>>builder().put(StorageType.MARIADB, ImmutableList.of(Dependency.MARIADB_DRIVER, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI)).put(StorageType.MYSQL, ImmutableList.of(Dependency.MYSQL_DRIVER, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI)).put(StorageType.SQLITE, ImmutableList.of(Dependency.SQLITE_DRIVER, Dependency.SLF4J_API, Dependency.SLF4J_SIMPLE, Dependency.HIKARI)).build();

    public Set<Dependency> resolveStorageDependencies(StorageType storageType) {
        Set<Dependency> dependencies = new LinkedHashSet<>();
        dependencies.addAll(STORAGE_DEPENDENCIES.get(storageType));
        if (core.getConfig().getMessagingService().equals(MessagingService.REDIS)) {
            LinkedHashSet<Dependency> jedis = new LinkedHashSet<>();
            jedis.add(Dependency.COMMONS_POOL_2);
            jedis.add(Dependency.JEDIS);
            dependencies.addAll(jedis);
            core.log("Loading messaging service dependencies: " + jedis);
        }
        if (classExists("org.slf4j.Logger") && classExists("org.slf4j.LoggerFactory")) {
            dependencies.remove(Dependency.SLF4J_API);
            dependencies.remove(Dependency.SLF4J_SIMPLE);
        }
        return dependencies;
    }

    private boolean classExists(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    public boolean shouldAutoLoad(Dependency dependency) {
        switch (dependency) {
            case ASM:
            case ASM_COMMONS:
            case JAR_RELOCATOR:
            case SQLITE_DRIVER:
                return false;
            default:
                return true;
        }
    }
}
