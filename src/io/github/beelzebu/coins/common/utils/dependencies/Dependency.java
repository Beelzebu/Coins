/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package io.github.beelzebu.coins.common.utils.dependencies;

public enum Dependency {

    CAFFEINE(
            "com{}github{}ben-manes{}caffeine",
            "caffeine",
            "2.6.2"
    ),
    COMMONS_IO(
            "commons-io",
            "commons-io",
            "2.6"
    ),
    MARIADB_DRIVER(
            "org{}mariadb{}jdbc",
            "mariadb-java-client",
            "2.2.3"
    ),
    MYSQL_DRIVER(
            "mysql",
            "mysql-connector-java",
            "5.1.46"
    ),
    SQLITE_DRIVER(
            "org.xerial",
            "sqlite-jdbc",
            "3.21.0"
    ),
    HIKARI(
            "com{}zaxxer",
            "HikariCP",
            "3.1.0"
    ),
    SLF4J_SIMPLE(
            "org.slf4j",
            "slf4j-simple",
            "1.7.25"
    ),
    SLF4J_API(
            "org.slf4j",
            "slf4j-api",
            "1.7.25"
    ),
    JEDIS(
            "redis.clients",
            "jedis",
            "2.9.0"
    ),
    COMMONS_POOL_2(
            "org.apache.commons",
            "commons-pool2",
            "2.5.0"
    );

    private final String url;
    private final String version;

    private static final String MAVEN_CENTRAL_FORMAT = "https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar";

    Dependency(String groupId, String artifactId, String version) {
        this(String.format(MAVEN_CENTRAL_FORMAT, rewriteEscaping(groupId).replace(".", "/"), rewriteEscaping(artifactId), version, rewriteEscaping(artifactId), version), version);
    }

    Dependency(String url, String version) {
        this.url = url;
        this.version = version;
    }

    private static String rewriteEscaping(String s) {
        return s.replace("{}", ".");
    }

    public String getUrl() {
        return this.url;
    }

    public String getVersion() {
        return this.version;
    }
}
