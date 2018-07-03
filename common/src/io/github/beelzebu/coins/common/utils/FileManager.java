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
package io.github.beelzebu.coins.common.utils;

import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import lombok.Getter;

/**
 * @author Beelzebu
 */
@Getter
public class FileManager {

    private final CoinsPlugin plugin;
    private final File messagesFolder;
    private final File logsFolder;
    private final File configFile;
    private final Map<String, File> messagesFiles = new HashMap<>();
    private final int configVersion = 15;

    public FileManager(CoinsPlugin plugin) {
        this.plugin = plugin;
        messagesFolder = new File(plugin.getBootstrap().getDataFolder(), "messages");
        logsFolder = new File(plugin.getBootstrap().getDataFolder(), "logs");
        configFile = new File(plugin.getBootstrap().getDataFolder(), "config.yml");
        messagesFiles.put("default", new File(messagesFolder, "messages.yml"));
        messagesFiles.put("es", new File(messagesFolder, "messages_es.yml"));
        messagesFiles.put("zh", new File(messagesFolder, "messages_zh.yml"));
        messagesFiles.put("cz", new File(messagesFolder, "messages_cz.yml"));
        messagesFiles.put("hu", new File(messagesFolder, "messages_hu.yml"));
        messagesFiles.put("ru", new File(messagesFolder, "messages_ru.yml"));
        //updateFiles();
    }

    private void updateConfig() {
        try {
            List<String> lines = Files.readAllLines(configFile.toPath());
            if (plugin.getConfig().getInt("version") == configVersion) {
                plugin.log("The config file is up to date.");
            } else {
                int version = configVersion;
                do {
                    int index;
                    switch (version) {
                        case 13:
                            index = lines.indexOf(" Fail:");
                            if (index != -1) {
                                String fix = lines.get(index + 1);
                                if (fix.startsWith(" Sound:")) {
                                    lines.set(index + 1, "    " + fix);
                                }
                                fix = lines.get(index + 2);
                                if (fix.startsWith("  Pitch:")) {
                                    lines.set(index + 2, "    " + fix);
                                }
                            }
                            index = lines.indexOf("MySQL:");
                            lines.addAll(index - 1, Arrays.asList("",
                                    "# Wich storage method the plugin should use.",
                                    "#",
                                    "# Available options:",
                                    "#  -> sqlite    data is stored locally and can't be shared with other servers.",
                                    "#  -> mysql     data is stored on a mysql server and can be shared by several servers.",
                                    "#  -> mariadb   we will use mariadb driver instead of mysql driver.",
                                    "Storage Type: sqlite",
                                    "",
                                    "# Don't touch this setting, this is only for internal usage to auto update the",
                                    "# database when something changes.",
                                    "Database Version: 1",
                                    "",
                                    "# Settings for messaging service",
                                    "# If enabled and configured, Coins will use the messaging service to inform other",
                                    "# connected servers of changes.",
                                    "#",
                                    "# Available options:",
                                    "#  -> bungeecord   uses the plugin messaging channels. You must enable bungeecord",
                                    "#                  in spigot.yml and install the plugin in BungeeCord to work.",
                                    "#  -> redis        uses redis pub sub to push changes. You redis server must be",
                                    "#                  configured below.",
                                    "#  -> none         nothing.",
                                    "Messaging Service: none"
                            ));
                            index = lines.indexOf("  Use: true");
                            if (index != -1) {
                                lines.remove(index);
                                index = lines.indexOf("Storage Type: sqlite");
                                lines.set(index, "Storage Type: mysql");
                            } else {
                                index = lines.indexOf("  Use: false");
                                lines.remove(index);
                            }
                            if (plugin.getConfig().useBungee()) {
                                index = lines.indexOf("Messaging Service: none");
                                lines.set(index, "Messaging Service: bungeecord");
                            }
                            index = lines.indexOf("MySQL:") + 8;
                            lines.addAll(index, Arrays.asList(
                                    "  # Don't change this value if you don't know what it does.",
                                    "  Connection Pool: 8",
                                    "  # MySQL table names without prefix, you can change this to use same database",
                                    "  # for all servers and but keep different balances in every server.",
                                    "  Data Table: 'data'",
                                    "  Multipliers Table: 'multipliers'",
                                    "# Here are the Redis server settings.",
                                    "Redis:",
                                    "  Host: 'localhost'",
                                    "  Port: 6379",
                                    "  Password: 'S3CUR3P4SSW0RD'"
                            ));
                            index = lines.indexOf("  Connection Interval: " + plugin.getConfig().getInt("MySQL.Connection Interval"));
                            lines.remove(index);
                            index = lines.indexOf("version: 13");
                            lines.set(index, "version: 14");
                            plugin.log("Configuration file updated to v14");
                            version++;
                        case 14:
                            // move executors to his own file
                            // move multipliers to his own file
                            index = lines.indexOf("Command Cost:");
                            if (index != -1) {
                                index += plugin.getConfig().getConfigurationSection("Command Cost").size();
                            }
                            for (int i = index; i < lines.size(); i++) {
                                lines.remove(i);
                            }
                            index = lines.indexOf("version: 14");
                            lines.set(index, "version: 15");
                            plugin.log("Configuration file updated to v15");
                            version++;
                        case configVersion:
                            break;
                        default:
                            plugin.log("Seems that you hava a too old version of the config or you canged this to another number >:(");
                            plugin.log("We can't update it, if is a old version you should try to update it slow and not jump from a version to another, keep in mind that we keep only track of the last versions of the config to update.");
                            break;
                    }
                    Files.write(configFile.toPath(), lines);
                    plugin.getConfig().reload();
                    version = plugin.getConfig().getInt("version");
                } while (version < configVersion && version > configVersion - 5);
            }
        } catch (IOException ex) {
            plugin.log("An unexpected error occurred while updating the config file.");
            plugin.debug(ex.getMessage());
        }
    }

    private void updateMessages() {
        try {
            List<String> lines = Files.readAllLines(messagesFiles.get("default").toPath());
            int index;
            if (plugin.getMessages("").getInt("version") == 6) {
                index = lines.indexOf("version: 6");
                lines.set(index, "version: 7");
                index = lines.indexOf("Multipliers:");
                lines.remove(index);
                index = lines.indexOf("  Menu:");
                lines.add(index, "Multipliers:");
                plugin.log("Updated messages.yml file to v7");
            }
            if (plugin.getMessages("").getInt("version") == 7) {
                index = lines.indexOf("version: 7");
                lines.set(index, "version: 8");
                index = lines.indexOf("  Menu:") + 2;
                lines.addAll(index, Arrays.asList(
                        "    Confirm:",
                        "      Title: '&8Are you sure?'",
                        "      Accept: '&a¡YES!'",
                        "      Decline: '&cNope'",
                        "    Multipliers:",
                        "      Name: '&6Multiplier &cx%amount%'",
                        "      Lore:",
                        "      - ''",
                        "      - '&7Amount: &c%amount%'",
                        "      - '&7Server: &c%server%'",
                        "      - '&7Minutes: &c%minutes%'",
                        "      - ''",
                        "      - '&7ID: &c#%id%'",
                        "    No Multipliers:",
                        "      Name: '&cYou don''t have any multiplier :('",
                        "      Lore:",
                        "      - ''",
                        "      - '&7You can buy multipliers in our store'",
                        "      - '&6&nstore.servername.net'"
                ));
                plugin.log("Updated messages.yml file to v8");
            }
            if (plugin.getMessages("").getInt("version") == 8) {
                index = lines.indexOf("version: 8");
                lines.set(index, "version: 9");
                lines.removeAll(Arrays.asList(
                        "# Coins messages file.",
                        "# If you need support or find a bug open a issuse in",
                        "# the official github repo https://github.com/Beelzebu/Coins/issuses/",
                        "",
                        "# The version of this file, don't edit!"
                ));
                lines.addAll(0, Arrays.asList(
                        "# Coins messages file.",
                        "# If you need support or find a bug open a issuse in",
                        "# the official github repo https://github.com/Beelzebu/Coins/issuses/",
                        "",
                        "# The version of this file, is used to auto update this file, don't change it",
                        "# unless you know what you do."
                ));
                plugin.log("Updated messages.yml file to v9");
            }
            if (plugin.getMessages("").getInt("version") == 9) {
                index = lines.indexOf("version: 9");
                lines.set(index, "version: 10");
                index = lines.indexOf("  Multiplier Create: '" + plugin.getMessages("").getString("Help.Multiplier Create") + "'") + 1;
                lines.add(index, "  Multiplier Set: '%prefix% &cPlease use &f/coins multiplier set <amount> <enabler> <minutes> (server)'");
                lines.addAll(Arrays.asList(
                        "  Set:",
                        "  - '%prefix% A multiplier with the following data was set for %server%'",
                        "  - '  &7Enabler: &c%enabler%'",
                        "  - '  &7Amount: &c%amount%'",
                        "  - '  &7Minutes: &c%minutes%'"
                ));
                plugin.log("Updated messages.yml file to v10");
            }
            Files.write(messagesFiles.get("default").toPath(), lines);
        } catch (IOException ex) {
            plugin.log("An unexpected error occurred while updating the messages.yml file.");
            plugin.debug(ex.getMessage());
        }
        try {
            List<String> lines = Files.readAllLines(messagesFiles.get("es").toPath());
            int index;
            if (plugin.getMessages("es").getInt("version") == 6) {
                index = lines.indexOf("version: 6");
                lines.set(index, "version: 7");
                index = lines.indexOf("Multipliers:");
                lines.remove(index);
                index = lines.indexOf("  Menu:");
                lines.add(index, "Multipliers:");
                plugin.log("Updated messages_es.yml file to v7");
            }
            if (plugin.getMessages("es").getInt("version") == 7) {
                index = lines.indexOf("version: 7");
                lines.set(index, "version: 8");
                index = lines.indexOf("  Menu:") + 2;
                lines.addAll(index, Arrays.asList(
                        "    Confirm:",
                        "      Title: '&8¿Estás seguro?'",
                        "      Accept: '&a¡SI!'",
                        "      Decline: '&cNo'",
                        "    Multipliers:",
                        "      Name: '&6Multiplicador &cx%amount%'",
                        "      Lore:",
                        "      - ''",
                        "      - '&7Cantidad: &c%amount%'",
                        "      - '&7Servidor: &c%server%'",
                        "      - '&7Minutos: &c%minutes%'",
                        "      - ''",
                        "      - '&7ID: &c#%id%'",
                        "    No Multipliers:",
                        "      Name: '&cNo tienes ningún multiplicador :('",
                        "      Lore:",
                        "      - ''",
                        "      - '&7Puedes comprar multiplicadores en nuestra tienda'",
                        "      - '&6&nstore.servername.net'"
                ));
                plugin.log("Updated messages_es.yml file to v8");
            }
            index = lines.indexOf("      - '&6&nstore.servername.net'\"");
            if (index != -1) {
                lines.set(index, "      - '&6&nstore.servername.net'");
            }
            if (plugin.getMessages("es").getInt("version") == 8) {
                index = lines.indexOf("version: 8");
                lines.set(index, "version: 9");
                lines.removeAll(Arrays.asList(
                        "# Coins messages file.",
                        "# If you need support or find a bug open a issuse in",
                        "# the official github repo https://github.com/Beelzebu/Coins/issuses/",
                        "",
                        "# The version of this file, don't edit!"
                ));
                lines.addAll(0, Arrays.asList(
                        "# Coins messages file.",
                        "# Si necesitas soporte o encuentras un error por favor abre un ticket en el",
                        "# repositorio oficial de github https://github.com/Beelzebu/Coins/issuses/",
                        "",
                        "# La versión de este archivo, es usado para actualizarlo automáticamente, no lo cambies",
                        "# a menos que sepas lo que haces."
                ));
                plugin.log("Updated messages_es.yml file to v9");
            }
            if (plugin.getMessages("es").getInt("version") == 9) {
                index = lines.indexOf("version: 9");
                lines.set(index, "version: 10");
                index = lines.indexOf("  Multiplier Create: '" + plugin.getMessages("es").getString("Help.Multiplier Create") + "'") + 1;
                lines.add(index, "  Multiplier Set: '%prefix% &cPor favor usa &f/coins multiplier set <cantidad> <activador> <minutos> (server)'");
                lines.addAll(Arrays.asList(
                        "  Set:",
                        "  - '%prefix% Un multiplicador con la siguiente información fue establecido en %server%'",
                        "  - '  &7Activador: &c%enabler%'",
                        "  - '  &7Cantidad: &c%amount%'",
                        "  - '  &7Minutos: &c%minutes%'"
                ));
                plugin.log("Updated messages_es.yml file to v10");
            }
            if (lines.get(0).startsWith("  Multiplier Set:")) {
                lines.remove(0);
                index = lines.indexOf("  Multiplier Create: '" + plugin.getMessages("es").getString("Help.Multiplier Create") + "'") + 1;
                lines.add(index, "  Multiplier Set: '%prefix% &cPor favor usa &f/coins multiplier set <cantidad> <activador> <minutos> (server)'");
            }
            Files.write(messagesFiles.get("es").toPath(), lines);
        } catch (IOException ex) {
            plugin.log("An unexpected error occurred while updating the messages_es.yml file.");
            plugin.debug(ex.getMessage());
        }
    }

    public void copyFiles() {
        if (!logsFolder.exists()) {
            if (plugin.getConfig() == null || plugin.getConfig().isDebugFile()) {
                logsFolder.mkdirs();
            } else {
                logsFolder.delete();
            }
        }
        if (!messagesFolder.exists()) {
            messagesFolder.mkdirs();
        }
        File[] files = plugin.getBootstrap().getDataFolder().listFiles();
        for (File f : files) {
            if (f.isFile() && f.getName().startsWith("messages")) {
                try {
                    Files.move(f.toPath(), new File(messagesFolder, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ex) {
                    Logger.getLogger(FileManager.class.getName()).log(Level.SEVERE, "An error has occurred while moving messages files to the new messages folder.", ex);
                }
            }
        }
        messagesFiles.keySet().forEach(filename -> {
            try {
                File messageFile = new File(messagesFolder, messagesFiles.get(filename).getName());
                if (!messageFile.exists()) {
                    Files.copy(plugin.getBootstrap().getResource(messagesFiles.get(filename).getName()), messageFile.toPath());
                }
            } catch (IOException ex) {
                Logger.getLogger(FileManager.class.getName()).log(Level.SEVERE, "An error has occurred while saving messages files.", ex);
            }
        });
        if (!configFile.exists()) {
            try {
                Files.copy(plugin.getBootstrap().getResource(configFile.getName()), configFile.toPath());
            } catch (IOException ex) {
                Logger.getLogger(FileManager.class.getName()).log(Level.SEVERE, "An error has occurred while saving the default config.", ex);
            }
        }
    }

    public void updateFiles() {
        updateMessages();
        updateConfig();
        checkLogs();
        messagesFiles.clear();
    }

    private void checkLogs() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        File latestLog = new File(logsFolder, "latest.log");
        if (latestLog.exists()) {
            try {
                int filen = 1;
                while (new File(logsFolder, sdf.format(latestLog.lastModified()) + "-" + filen + ".log.gz").exists()) {
                    filen++;
                }
                gzipFile(Files.newInputStream(latestLog.toPath()), logsFolder + File.pathSeparator + sdf.format(latestLog.lastModified()) + "-" + filen + ".log.gz");
                latestLog.delete();
            } catch (IOException ex) {
                Logger.getLogger(FileManager.class.getName()).log(Level.WARNING, "An unexpected error has occurred while trying to compress the latest log file. {0}", ex.getMessage());
            }
        }
        File[] fList = logsFolder.listFiles();
        // Auto purge for old logs
        if (fList.length > 0) {
            for (File file : fList) {
                if (file.isFile() && file.getName().contains(".gz") && (System.currentTimeMillis() - file.lastModified()) >= plugin.getConfig().getInt("General.Purge.Logs.Days") * 86400000L) {
                    file.delete();
                }
            }
        }
    }

    private void gzipFile(InputStream in, String to) throws IOException {
        try (GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(to))) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            in.close();
        }
    }

    public void updateDatabaseVersion(int version) {
        if (plugin.getConfig().getInt("Database Version") != version) {
            try {
                List<String> lines = Files.readAllLines(configFile.toPath());
                int index = lines.indexOf("Database Version: " + plugin.getConfig().getInt("Database Version"));
                lines.set(index, "Database Version: " + version);
                Files.write(configFile.toPath(), lines);
                plugin.getConfig().reload();
            } catch (IOException ex) {
                plugin.log("An unexpected error occurred while updating the config file.");
                plugin.debug(ex.getMessage());
            }
        }
    }
}
