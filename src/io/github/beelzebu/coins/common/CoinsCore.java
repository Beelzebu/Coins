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
package io.github.beelzebu.coins.common;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.bungee.BungeeMethods;
import io.github.beelzebu.coins.common.database.CoinsDatabase;
import io.github.beelzebu.coins.common.database.MySQL;
import io.github.beelzebu.coins.common.database.Redis;
import io.github.beelzebu.coins.common.database.SQLite;
import io.github.beelzebu.coins.common.database.StorageType;
import io.github.beelzebu.coins.common.executor.ExecutorManager;
import io.github.beelzebu.coins.common.interfaces.IMethods;
import io.github.beelzebu.coins.common.utils.CoinsConfig;
import io.github.beelzebu.coins.common.utils.FileManager;
import io.github.beelzebu.coins.common.utils.MessagesManager;
import io.github.beelzebu.coins.common.utils.MessagingService;
import io.github.beelzebu.coins.common.utils.dependencies.DependencyManager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Beelzebu
 */
public class CoinsCore {

    private static CoinsCore instance;
    @Getter
    private IMethods methods;
    @Getter
    private FileManager fileUpdater;
    private CoinsDatabase db;
    @Getter
    private Redis redis;
    @Getter
    private StorageType storageType;
    @Getter
    private ExecutorManager executorManager;
    private HashMap<String, MessagesManager> messagesMap;
    @Getter
    private final Gson gson = new Gson();

    public static CoinsCore getInstance() {
        return instance == null ? instance = new CoinsCore() : instance;
    }

    public void setup(IMethods imethods) {
        methods = imethods;
        fileUpdater = new FileManager(this);
        fileUpdater.copyFiles();
        methods.loadConfig();
        fileUpdater.updateFiles();
        try {
            if (getConfig().getBoolean("MySQL.Use")) {
                storageType = StorageType.MYSQL;
            } else {
                storageType = StorageType.valueOf(getConfig().getString("Storage Type", "SQLITE").toUpperCase());
            }
        } catch (Exception ex) { // invalid storage type
            storageType = StorageType.SQLITE;
            log("Invalid Storage Type selected in the config, possible values: " + Arrays.toString(StorageType.values()));
        }
        DependencyManager.loadAllDependencies();
        messagesMap = new HashMap<>();
    }

    public void shutdown() {
        db.shutdown();
        motd(false);
    }

    public void start() {
        if (storageType.equals(StorageType.SQLITE) && isBungee()) {
            log(" ");
            log("    WARNING");
            log(" ");
            log("Bungeecord doesn't support SQLite storage, change it to MySQL and reload the plugin.");
            log(" ");
            log("    WARNING");
            log(" ");
            return;
        }
        getConfig().reload();
        motd(true);
        try {
            Iterator<String> lines = FileUtils.readLines(new File(methods.getDataFolder(), "multipliers.json"), Charsets.UTF_8).iterator();
            while (lines.hasNext()) {
                try {
                    Multiplier multiplier = Multiplier.fromJson(lines.next(), false);
                    CacheManager.getMultipliersData().put(multiplier.getServer().toLowerCase(), multiplier);
                } catch (Exception ignore) { // Invalid line
                }
            }
        } catch (IOException ex) {
            log("An error has ocurred loading multipliers from local storage.");
            debug(ex.getMessage());
        }
        if (getConfig().getMessagingService().equals(MessagingService.REDIS)) {
            if (storageType.equals(StorageType.REDIS)) {
                redis = (Redis) getDatabase();
            } else {
                redis = new Redis();
                redis.setup();
            }
        }
        if (storageType.equals(StorageType.SQLITE) && getConfig().getInt("Database Version", 1) < 2) {
            try {
                Files.move(new File(methods.getDataFolder(), "database.db").toPath(), new File(methods.getDataFolder(), "database.old.db").toPath());
            } catch (IOException ex) {
                log("An error has ocurred moving the old database");
                debug(ex.getMessage());
            }
        }
        getDatabase().setup();
        executorManager = new ExecutorManager();
    }

    private void motd(boolean enable) {
        methods.sendMessage(methods.getConsole(), rep(""));
        methods.sendMessage(methods.getConsole(), rep("&6-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"));
        methods.sendMessage(methods.getConsole(), rep("           &4Coins &fBy:  &7Beelzebu"));
        methods.sendMessage(methods.getConsole(), rep(""));
        StringBuilder version = new StringBuilder();
        int spaces = (42 - ("v: " + methods.getVersion()).length()) / 2;
        for (int i = 0; i < spaces; i++) {
            version.append(" ");
        }
        version.append(rep("&4v: &f" + methods.getVersion()));
        methods.sendMessage(methods.getConsole(), version.toString());
        methods.sendMessage(methods.getConsole(), rep("&6-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"));
        methods.sendMessage(methods.getConsole(), rep(""));
        // Only send this in the onEnable
        if (enable) {
            if (getConfig().getBoolean("Debug", false)) {
                log("Debug mode is enabled.");
            }
            log("Using \"" + storageType.toString().toLowerCase() + "\" for storage.");
            log("Using \"" + getConfig().getMessagingService().toString().toLowerCase() + "\" as messaging service.");
            String upt = "You have the newest version";
            String response = getFromURL("https://api.spigotmc.org/legacy/update.php?resource=48536");
            if (response == null) {
                upt = "Failed to check for updates :(";
            } else if (!response.equalsIgnoreCase(methods.getVersion())) {
                upt = "There is a new version available! [" + response + "]";
            }
            log(upt);

        }
    }

    public void debug(Object msg) {
        if (getConfig().getBoolean("Debug")) {
            methods.sendMessage(methods.getConsole(), rep("&8[&cCoins&8] &cDebug: &7" + msg));
        }
        logToFile(msg);
    }

    public void debug(SQLException ex) {
        debug("SQLException:");
        debug("   Database state: " + ex.getSQLState());
        debug("   Error code: " + ex.getErrorCode());
        debug("   Error message: " + ex.getMessage());
    }

    public void debug(Exception ex) {
        debug("Unknown Exception:");
        debug("   Error message: " + ex.getMessage());
    }

    public void log(Object msg) {
        methods.log(msg);
        logToFile(msg);
    }

    private void logToFile(Object msg) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        File log = new File(methods.getDataFolder(), "/logs/latest.log");
        if (!log.exists()) {
            try {
                log.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(CoinsCore.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(log, true))) {
            try {
                writer.write("[" + sdf.format(System.currentTimeMillis()) + "] " + removeColor(msg.toString()));
                writer.newLine();
            } finally {
                writer.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(CoinsCore.class.getName()).log(Level.WARNING, "Can''t save the debug to the file", ex);
        }
    }

    @Deprecated
    public String getNick(UUID uuid) {
        return getNick(uuid, false);
    }

    public String getNick(UUID uuid, boolean fromdb) {
        if (!fromdb && methods.getName(uuid) != null) {
            return methods.getName(uuid);
        }
        return getDatabase().getNick(uuid);
    }

    @Deprecated
    public UUID getUUID(String name) {
        return getUUID(name, false);
    }

    public UUID getUUID(String name, boolean fromdb) {
        if (!fromdb && methods.getUUID(name) != null) {
            return methods.getUUID(name);
        }
        return getDatabase().getUUID(name.toLowerCase());
    }

    public CoinsDatabase getDatabase() {
        switch (storageType) {
            case MYSQL:
                return db == null ? db = new MySQL() : db;
            case SQLITE:
                return db == null ? db = new SQLite() : db;
            case REDIS:
                return db == null ? db = new Redis() : db;
            default:
                return null;
        }
    }

    public String rep(String msg) {
        if (msg == null) {
            return "";
        }
        String message = msg;
        if (getConfig() != null) {
            message = message.replaceAll("%prefix%", getConfig().getString("Prefix", "&c&lCoins &6&l>&7"));
        }
        return message.replaceAll("&", "§");
    }

    public String rep(String msg, Multiplier multiplier) {
        String string = msg;
        if (multiplier != null) {
            string = msg.replaceAll("%enabler%", multiplier.getEnablerName())
                    .replaceAll("%server%", multiplier.getServer())
                    .replaceAll("%amount%", String.valueOf(multiplier.getAmount()))
                    .replaceAll("%minutes%", String.valueOf(multiplier.getMinutes()))
                    .replaceAll("%id%", String.valueOf(multiplier.getId()));
        }
        return rep(string);
    }

    public List<String> rep(List<String> msgs) {
        List<String> message = new ArrayList<>();
        msgs.forEach(msg -> message.add(rep(msg)));
        return message;
    }

    public List<String> rep(List<String> msgs, Multiplier multiplierData) {
        List<String> message = new ArrayList<>();
        msgs.forEach(msg -> message.add(rep(msg, multiplierData)));
        return message;
    }

    public String removeColor(String str) {
        return ChatColor.stripColor(rep(str)).replaceAll("Debug: ", "");
    }

    public CoinsConfig getConfig() {
        return methods.getConfig();
    }

    public MessagesManager getMessages(String lang) {
        if (lang == null || lang.equals("")) {
            lang = "default";
        }
        lang = lang.split("_")[0];
        if (!messagesMap.containsKey(lang)) {
            if (new File(methods.getDataFolder() + "/messages", "messages_" + lang + ".yml").exists()) {
                messagesMap.put(lang, methods.getMessages("_" + lang));
            } else {
                messagesMap.put(lang, methods.getMessages(""));
            }
        }
        return messagesMap.get(lang);
    }

    public String getString(String path, String lang) {
        try {
            return rep(getMessages(lang).getString(path));
        } catch (NullPointerException ex) {
            methods.log("The string " + path + " does not exists in the messages_" + lang.split("_")[0] + ".yml file.");
            debug(ex);
            return rep(getMessages("").getString(path, ""));
        }
    }

    public boolean isBungee() {
        return methods instanceof BungeeMethods;
    }

    public void reloadMessages() {
        messagesMap.keySet().forEach(lang -> messagesMap.get(lang).reload());
    }

    public String getFromURL(String surl) {
        String response = null;
        try {
            URL url = new URL(surl);
            Scanner s = new Scanner(url.openStream());
            if (s.hasNext()) {
                response = s.next();
                s.close();
            }
        } catch (IOException exc) {
            debug("Failed to connect to URL: " + surl);
        }
        return response;
    }
}
