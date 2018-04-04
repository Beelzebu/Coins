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

import com.google.common.base.Charsets;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import io.github.beelzebu.coins.common.Core;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Beelzebu
 */
public class FileManager {

    private final Core core;
    private final File messagesFolder;
    private final Map<String, File> messagesFiles;
    private final File configFile;
    private final File logsFolder;

    public FileManager(Core c) {
        core = c;
        messagesFolder = new File(core.getDataFolder(), "messages");
        messagesFiles = new HashMap<>();
        messagesFiles.put("default", new File(messagesFolder, "messages.yml"));
        messagesFiles.put("es", new File(messagesFolder, "messages_es.yml"));
        messagesFiles.put("zh", new File(messagesFolder, "messages_zh.yml"));
        messagesFiles.put("cz", new File(messagesFolder, "messages_cz.yml"));
        messagesFiles.put("hu", new File(messagesFolder, "messages_hu.yml"));
        messagesFiles.put("ru", new File(messagesFolder, "messages_ru.yml"));
        configFile = new File(core.getDataFolder(), "config.yml");
        logsFolder = new File(core.getDataFolder(), "logs");
    }

    public void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (IOException e) {
            Logger.getLogger(FileManager.class.getName()).log(Level.WARNING, "Can''t copy the file {0} to the plugin data folder. {1}", new Object[]{file.getName(), e.getMessage()});
        }
    }

    private void updateConfig() {
        try {
            List<String> lines = FileUtils.readLines(configFile, Charsets.UTF_8);
            int index;
            if (core.getConfig().getInt("version") == 13) {
                core.log("The config file is up to date.");
            } else {
                switch (core.getConfig().getInt("version")) {
                    case 8:
                        index = lines.indexOf("  Purge:") + 5;
                        lines.addAll(index, Arrays.asList(
                                "  Executor Sign:",
                                "    1: '&c&lCoins'",
                                "    2: '%executor_displayname%'",
                                "    3: '%ececutor_cost%'",
                                "    4: ''"
                        ));
                        index = lines.indexOf("version: 8");
                        lines.set(index, "version: 9");
                        core.log("Configuration file updated to v9");
                        break;
                    case 9:
                        index = lines.indexOf("MySQL:") - 2;
                        lines.addAll(index, Arrays.asList(
                                "# Here you can enable Vault to make this plugin manage all the Vault transactions.",
                                "Vault:",
                                "  Use: false",
                                "  # Names used by vault for the currency.",
                                "  Name:",
                                "    Singular: 'Coin'",
                                "    Plural: 'Coins'",
                                ""
                        ));
                        index = lines.indexOf("version: 9");
                        lines.set(index, "version: 10");
                        core.log("Configuraton file updated to v10");
                        break;
                    case 10:
                        index = lines.indexOf("    Close:") + 1;
                        lines.addAll(index, Arrays.asList(
                                "      # To see all possible values check https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html",
                                "      Material: REDSTONE_BLOCK",
                                "      Name: '&c&lClose'",
                                "      Lore:",
                                "      - ''",
                                "      - '&7Click me to close this gui'"
                        ));
                        index = lines.indexOf("version: 10");
                        lines.set(index, "version: 11");
                        core.log("Configuraton file updated to v11");
                        break;
                    case 11:
                        index = lines.indexOf("  Executor Sign:") + 5;
                        lines.addAll(index, Arrays.asList(
                                "  # If you want the users to be created when they join to the server, enable this,",
                                "  # otherwise the players will be created when his coins are modified or consulted",
                                "  # to the database for the first time (recommended for big servers).",
                                "  Create Join: false"
                        ));
                        index = lines.indexOf("version: 11");
                        lines.set(index, "version: 12");
                        core.log("Configuration file updated to v12");
                        break;
                    case 12:
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
                                "#  -> redis     data is stored on a redis server and can be shared by several servers.",
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
                        if (core.isBungee() || core.getConfig().useBungee()) {
                            index = lines.indexOf("Messaging Service: none");
                            lines.set(index, "Messaging Service: bungeecord");
                        }
                        index = lines.indexOf("MySQL:") + 8;
                        lines.addAll(index, Arrays.asList("  # Don't change this value if you don't know what it does.",
                                "  Connection Pool: 8",
                                "  ",
                                "# Here are the Redis server settings.",
                                "Redis:",
                                "  Host: 'localhost'",
                                "  Port: 6379",
                                "  Password: 'S3CUR3P4SSW0RD'"
                        ));
                        index = lines.indexOf("version: 12");
                        lines.set(index, "version: 13");
                        core.log("Configuration file updated to v13");
                        break;
                    default:
                        core.log("Seems that you hava a too old version of the config or you canged this to another number >:(");
                        core.log("We can't update it, if is a old version you should try to update it slow and not jump from a version to another, keep in mind that we keep track of the last 3 versions of the config to update.");
                        break;
                }
            }
            FileUtils.writeLines(configFile, lines);
            core.getConfig().reload();
        } catch (IOException ex) {
            core.log("An unexpected error occurred while updating the config file.");
            core.debug(ex.getMessage());
        }
    }

    private void updateMessages() {
        try {
            for (Map.Entry<String, File> ent : messagesFiles.entrySet()) {
                List<String> liness = FileUtils.readLines(ent.getValue(), Charsets.UTF_8);
                Iterator<String> it = liness.iterator();
                while (it.hasNext()) {
                    String line = it.next();
                    if (line.contains("Unknow ")) {
                        liness.set(liness.indexOf(line), line.replaceAll("Unknow ", "Unknown "));
                    }
                }
                FileUtils.writeLines(ent.getValue(), liness);
            }
        } catch (IOException ex) {
            Logger.getLogger(FileManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            List<String> lines = FileUtils.readLines(messagesFiles.get("default"), Charsets.UTF_8);
            int index;
            if (core.getMessages("").getInt("version") == 6) {
                index = lines.indexOf("version: 6");
                lines.set(index, "version: 7");
                index = lines.indexOf("Multipliers:");
                lines.remove(index);
                index = lines.indexOf("  Menu:");
                lines.add(index, "Multipliers:");
                core.log("Updated messages.yml file to v7");
            }
            if (core.getMessages("").getInt("version") == 7) {
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
                core.log("Updated messages.yml file to v8");
            }
            if (core.getMessages("").getInt("version") == 8) {
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
                core.log("Updated messages.yml file to v9");
            }
            if (core.getMessages("").getInt("version") == 9) {
                index = lines.indexOf("version: 9");
                lines.set(index, "version: 10");
                index = lines.indexOf("  Multiplier Create: '" + core.getMessages("").getString("Help.Multiplier Create") + "'") + 1;
                lines.add(index, "  Multiplier Set: '%prefix% &cPlease use &f/coins multiplier set <amount> <enabler> <minutes> (server)'");
                lines.addAll(Arrays.asList(
                        "  Set:",
                        "  - '%prefix% A multiplier with the following data was set for %server%'",
                        "  - '  &7Enabler: &c%enabler%'",
                        "  - '  &7Amount: &c%amount%'",
                        "  - '  &7Minutes: &c%minutes%'"
                ));
                core.log("Updated messages.yml file to v10");
            }
            FileUtils.writeLines(messagesFiles.get("default"), lines);
        } catch (IOException ex) {
            core.log("An unexpected error occurred while updating the messages.yml file.");
            core.debug(ex.getMessage());
        }
        try {
            List<String> lines = FileUtils.readLines(messagesFiles.get("es"), Charsets.UTF_8);
            int index;
            if (core.getMessages("es").getInt("version") == 6) {
                index = lines.indexOf("version: 6");
                lines.set(index, "version: 7");
                index = lines.indexOf("Multipliers:");
                lines.remove(index);
                index = lines.indexOf("  Menu:");
                lines.add(index, "Multipliers:");
                core.log("Updated messages_es.yml file to v7");
            }
            if (core.getMessages("es").getInt("version") == 7) {
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
                core.log("Updated messages_es.yml file to v8");
            }
            index = lines.indexOf("      - '&6&nstore.servername.net'\"");
            if (index != -1) {
                lines.set(index, "      - '&6&nstore.servername.net'");
            }
            if (core.getMessages("es").getInt("version") == 8) {
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
                core.log("Updated messages_es.yml file to v9");
            }
            if (core.getMessages("es").getInt("version") == 9) {
                index = lines.indexOf("version: 9");
                lines.set(index, "version: 10");
                index = lines.indexOf("  Multiplier Create: '" + core.getMessages("es").getString("Help.Multiplier Create") + "'") + 1;
                lines.add(index, "  Multiplier Set: '%prefix% &cPor favor usa &f/coins multiplier set <cantidad> <activador> <minutos> (server)'");
                lines.addAll(Arrays.asList(
                        "  Set:",
                        "  - '%prefix% Un multiplicador con la siguiente información fue establecido en %server%'",
                        "  - '  &7Activador: &c%enabler%'",
                        "  - '  &7Cantidad: &c%amount%'",
                        "  - '  &7Minutos: &c%minutes%'"
                ));
                core.log("Updated messages_es.yml file to v10");
            }
            if (lines.get(0).startsWith("  Multiplier Set:")) {
                lines.remove(0);
                index = lines.indexOf("  Multiplier Create: '" + core.getMessages("es").getString("Help.Multiplier Create") + "'") + 1;
                lines.add(index, "  Multiplier Set: '%prefix% &cPor favor usa &f/coins multiplier set <cantidad> <activador> <minutos> (server)'");
            }
            FileUtils.writeLines(messagesFiles.get("es"), lines);
        } catch (IOException ex) {
            core.log("An unexpected error occurred while updating the messages_es.yml file.");
            core.debug(ex.getMessage());
        }
    }

    public void copyFiles() {
        if (!messagesFolder.exists()) {
            messagesFolder.mkdirs();
        }
        {
            File[] files = core.getDataFolder().listFiles();
            for (File f : files) {
                if (f.isFile() && f.getName().startsWith("messages")) {
                    try {
                        Files.move(f.toPath(), new File(messagesFolder, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException ex) {
                        Logger.getLogger(FileManager.class.getName()).log(Level.SEVERE, "An error has ocurred while moving messages files to the new messages folder.", ex);
                    }
                }
            }
        }
        messagesFiles.keySet().forEach(filename -> {
            File messages = messagesFiles.get(filename);
            if (!messages.exists()) {
                copy(core.getResource(messages.getName()), messages);
            }
        });
        if (!configFile.exists()) {
            copy(core.getResource("config.yml"), configFile);
        }
    }

    public void updateFiles() {
        updateMessages();
        core.getConfig().reload();
        updateConfig();
        checkLogs();
    }

    private void checkLogs() {
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        File latestLog = new File(logsFolder, "latest.log");
        if (latestLog.exists()) {
            try {
                int filen = 1;
                while (new File(logsFolder, sdf.format(latestLog.lastModified()) + "-" + filen + ".log.gz").exists()) {
                    filen++;
                }
                gzipFile(Files.newInputStream(latestLog.toPath()), logsFolder + "/" + sdf.format(latestLog.lastModified()) + "-" + filen + ".log.gz");
                latestLog.delete();
            } catch (IOException ex) {
                Logger.getLogger(FileManager.class.getName()).log(Level.WARNING, "An unexpected error has ocurred while trying to compress the latest log file. {0}", ex.getMessage());
            }
        }
        File[] fList = logsFolder.listFiles();
        // Auto purge for old logs
        if (fList.length > 0) {
            for (File file : fList) {
                if (file.isFile() && file.getName().contains(".gz") && (System.currentTimeMillis() - file.lastModified()) >= core.getConfig().getInt("General.Purge.Logs.Days") * 86400000L) {
                    file.delete();
                }
            }
        }
    }

    private void gzipFile(InputStream in, String to) throws IOException {
        GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(to));
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
        in.close();
        out.close();
    }

    public void updateDatabaseVersion(int version) {
        if (core.getConfig().getInt("Database Version") != version) {
            try {
                List<String> lines = FileUtils.readLines(configFile, Charsets.UTF_8);
                int index = lines.indexOf("Database Version: " + core.getConfig().getInt("Database Version"));
                lines.set(index, "Database Version: " + version);
                FileUtils.writeLines(configFile, lines);
                core.getConfig().reload();
            } catch (IOException ex) {
                core.log("An unexpected error occurred while updating the config file.");
                core.debug(ex.getMessage());
            }
        }
    }
}
