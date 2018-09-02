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
package io.github.beelzebu.coins.api.dependency;

import io.github.beelzebu.coins.api.dependency.relocation.Relocation;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

@Getter
public enum Dependency {

    ASM("org.ow2.asm", "asm", "6.1.1"),
    ASM_COMMONS("org.ow2.asm", "asm-commons", "6.1.1"),
    JAR_RELOCATOR("me.lucko", "jar-relocator", "1.3"),
    CAFFEINE("com{}github{}ben-manes{}caffeine", "caffeine", "2.6.2", Relocation.of("caffeine", "com{}github{}benmanes{}caffeine")),
    MARIADB_DRIVER("org{}mariadb{}jdbc", "mariadb-java-client", "2.2.3", Relocation.of("mariadb", "org{}mariadb{}jdbc")),
    MYSQL_DRIVER("mysql", "mysql-connector-java", "5.1.46", Relocation.of("mysql", "com{}mysql")),
    SQLITE_DRIVER("org.xerial", "sqlite-jdbc", "3.21.0"),
    HIKARI("com{}zaxxer", "HikariCP", "3.2.0", Relocation.of("hikari", "com{}zaxxer{}hikari")),
    SLF4J_SIMPLE("org.slf4j", "slf4j-simple", "1.7.25"),
    SLF4J_API("org.slf4j", "slf4j-api", "1.7.25"),
    JEDIS("redis.clients", "jedis", "2.9.0", Relocation.allOf(Relocation.of("jedis", "redis{}clients{}jedis"), Relocation.of("jedisutil", "redis{}clients{}util"), Relocation.of("commonspool2", "org{}apache{}commons{}pool2"))),
    COMMONS_POOL_2("org.apache.commons", "commons-pool2", "2.5.0", Relocation.of("commonspool2", "org{}apache{}commons{}pool2"));

    private static final String MAVEN_CENTRAL_FORMAT = "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar";
    private final String url;
    private final String version;
    private final List<Relocation> relocations;

    Dependency(String groupId, String artifactId, String version) {
        this(String.format(MAVEN_CENTRAL_FORMAT, groupId.replace("{}", ".").replace(".", "/"), artifactId.replace("{}", "."), version, artifactId.replace("{}", "."), version), version, Collections.emptyList());
    }

    Dependency(String groupId, String artifactId, String version, Relocation relocations) {
        this(String.format(MAVEN_CENTRAL_FORMAT, groupId.replace("{}", ".").replace(".", "/"), artifactId.replace("{}", "."), version, artifactId.replace("{}", "."), version), version, Collections.singletonList(relocations));
    }

    Dependency(String groupId, String artifactId, String version, List<Relocation> relocations) {
        this(String.format(MAVEN_CENTRAL_FORMAT, groupId.replace("{}", ".").replace(".", "/"), artifactId.replace("{}", "."), version, artifactId.replace("{}", "."), version), version, relocations);
    }

    Dependency(String url, String version, List<Relocation> relocations) {
        this.url = url;
        this.version = version;
        this.relocations = relocations;
    }
}
