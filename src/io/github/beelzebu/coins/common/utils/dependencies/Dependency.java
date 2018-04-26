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
package io.github.beelzebu.coins.common.utils.dependencies;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 * @author Beelzebu
 */
@Getter
@AllArgsConstructor
public enum Dependency {
    MYSQL_DRIVER("mysql", "mysql-connector-java", "5.1.44"),
    SQLITE_DRIVER("org.xerial", "sqlite-jdbc", "3.20.0"),
    HIKARI("com.zaxxer", "HikariCP", "3.1.0"),
    SLF4J_SIMPLE("org.slf4j", "slf4j-simple", "1.7.25"),
    SLF4J_API("org.slf4j", "slf4j-api", "1.7.25"),
    COMMONS_IO("commons-io", "commons-io", "2.6"),
    JEDIS("redis.clients", "jedis", "2.9.0"),
    COMMONS_POOL("org.apache.commons", "commons-pool2", "2.4.2");

    private final String url;
    private final String version;

    private static final String MAVEN_CENTRAL_FORMAT = "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar";

    Dependency(String groupId, String artifactId, String version) {
        this(String.format(MAVEN_CENTRAL_FORMAT, groupId.replace(".", "/"), artifactId, version, artifactId, version), version);
    }
}
