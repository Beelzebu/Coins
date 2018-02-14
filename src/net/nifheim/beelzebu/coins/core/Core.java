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
package net.nifheim.beelzebu.coins.core;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.nifheim.beelzebu.coins.bukkit.utils.bungee.PluginMessage;
import net.nifheim.beelzebu.coins.bungee.BungeeMethods;
import net.nifheim.beelzebu.coins.bungee.listener.PluginMessageListener;
import net.nifheim.beelzebu.coins.core.database.CoinsDatabase;
import net.nifheim.beelzebu.coins.core.database.MySQL;
import net.nifheim.beelzebu.coins.core.database.Redis;
import net.nifheim.beelzebu.coins.core.database.SQLite;
import net.nifheim.beelzebu.coins.core.database.StorageType;
import net.nifheim.beelzebu.coins.core.executor.ExecutorManager;
import net.nifheim.beelzebu.coins.core.interfaces.IMethods;
import net.nifheim.beelzebu.coins.core.multiplier.Multiplier;
import net.nifheim.beelzebu.coins.core.utils.CoinsConfig;
import net.nifheim.beelzebu.coins.core.utils.FileManager;
import net.nifheim.beelzebu.coins.core.utils.MessagesManager;
import net.nifheim.beelzebu.coins.core.utils.MessagingService;
import net.nifheim.beelzebu.coins.core.utils.dependencies.DependencyManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;
import redis.clients.jedis.Jedis;

/**
 *
 * @author Beelzebu
 */
public class Core {

    private static Core instance;
    private IMethods mi;
    @Getter
    private FileManager fileUpdater;
    private CoinsDatabase db;
    @Getter
    private Redis redis;
    @Getter
    private StorageType storageType;
    private ExecutorManager executorManager;
    private HashMap<String, MessagesManager> messagesMap;
    @Getter
    private final Gson gson = new Gson();

    public static Core getInstance() {
        return instance == null ? instance = new Core() : instance;
    }

    public void setup(IMethods methodinterface) {
        mi = methodinterface;
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
        fileUpdater = new FileManager(this);
        fileUpdater.copyFiles();
        messagesMap = new HashMap<>();
        fileUpdater.updateFiles();
    }

    public void shutdown() {
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
            Iterator<String> lines = FileUtils.readLines(new File(getDataFolder(), "multipliers.json"), Charsets.UTF_8).iterator();
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
                Files.move(new File(getDataFolder(), "database.db").toPath(), new File(getDataFolder(), "database.old.db").toPath());
            } catch (IOException ex) {
                log("An error has ocurred moving the old database");
                debug(ex.getMessage());
            }
        }
        getDatabase().setup();
        executorManager = new ExecutorManager();
    }

    private void motd(boolean enable) {
        mi.sendMessage(mi.getConsole(), rep(""));
        mi.sendMessage(mi.getConsole(), rep("&6-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"));
        mi.sendMessage(mi.getConsole(), rep("           &4Coins &fBy:  &7Beelzebu"));
        mi.sendMessage(mi.getConsole(), rep(""));
        StringBuilder version = new StringBuilder();
        int spaces = (42 - ("v: " + mi.getVersion()).length()) / 2;
        for (int i = 0; i < spaces; i++) {
            version.append(" ");
        }
        version.append(rep("&4v: &f" + mi.getVersion()));
        mi.sendMessage(mi.getConsole(), version.toString());
        mi.sendMessage(mi.getConsole(), rep("&6-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"));
        mi.sendMessage(mi.getConsole(), rep(""));
        // Only send this in the onEnable
        if (enable) {
            if (getConfig().getBoolean("Debug", false)) {
                log("Debug mode is enabled.");
            }
            log("Using " + storageType + " for storage.");
            log("Using " + getConfig().getMessagingService() + " as messaging service.");
        }
    }

    public IMethods getMethods() {
        return mi;
    }

    public void debug(Object msg) {
        if (getConfig().getBoolean("Debug")) {
            mi.sendMessage(mi.getConsole(), rep("&8[&cCoins&8] &cDebug: &7" + msg));
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
        mi.log(msg);
        logToFile(msg);
    }

    private void logToFile(Object msg) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        File log = new File(getDataFolder(), "/logs/latest.log");
        if (!log.exists()) {
            try {
                log.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(Core.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(Core.class.getName()).log(Level.WARNING, "Can''t save the debug to the file", ex);
        }
    }

    public boolean isOnline(UUID uuid) {
        return mi.isOnline(uuid);
    }

    public boolean isOnline(String name) {
        return mi.isOnline(name);
    }

    @Deprecated
    public String getNick(UUID uuid) {
        return getNick(uuid, false);
    }

    public String getNick(UUID uuid, boolean fromdb) {
        if (!fromdb && mi.getName(uuid) != null) {
            return mi.getName(uuid);
        }
        return getDatabase().getNick(uuid);
    }

    @Deprecated
    public UUID getUUID(String name) {
        return getUUID(name, false);
    }

    public UUID getUUID(String name, boolean fromdb) {
        if (!fromdb && mi.getUUID(name) != null) {
            return mi.getUUID(name);
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
            message = message.replaceAll("%prefix%", getConfig().getString("Prefix"));
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
        return mi.getConfig();
    }

    public File getDataFolder() {
        return mi.getDataFolder();
    }

    public InputStream getResource(String filename) {
        return mi.getResource(filename);
    }

    public MessagesManager getMessages(String lang) {
        if (lang == null || lang.equals("")) {
            lang = "default";
        }
        lang = lang.split("_")[0];
        if (!messagesMap.containsKey(lang)) {
            if (new File(getDataFolder() + "/messages", "messages_" + lang + ".yml").exists()) {
                messagesMap.put(lang, mi.getMessages("_" + lang));
            } else {
                messagesMap.put(lang, mi.getMessages(""));
            }
        }
        return messagesMap.get(lang);
    }

    public String getString(String path, String lang) {
        try {
            return rep(getMessages(lang).getString(path));
        } catch (NullPointerException ex) {
            mi.log("The string " + path + " does not exists in the messages_" + lang.split("_")[0] + ".yml file.");
            debug(ex);
            return rep(getMessages("").getString(path));
        }
    }

    public ExecutorManager getExecutorManager() {
        return executorManager;
    }

    public boolean isBungee() {
        return mi instanceof BungeeMethods;
    }

    public void updateCache(UUID uuid, double amount) {
        Validate.notNull(uuid, "The uuid can't be null");
        if (getConfig().getMessagingService().equals(MessagingService.REDIS)) { // Update using redis pub/sub
            try (Jedis jedis = redis.getPool().getResource()) {
                jedis.publish("coins-data-update", uuid + " " + amount);
            }
        } else if (getConfig().getMessagingService().equals(MessagingService.BUNGEECORD)) {
            CacheManager.updateCoins(uuid, amount);
            if (isBungee()) { // Update using bungee or redisbungee if is present.
                ProxyServer.getInstance().getServers().keySet().forEach(server -> {
                    PluginMessageListener pml = new PluginMessageListener();
                    pml.sendToBukkit("Update", Collections.singletonList(uuid + " " + amount), ProxyServer.getInstance().getServerInfo(server), false);
                });
            } else if (getConfig().useBungee()) { // Update using plugin message
                PluginMessage pm = new PluginMessage();
                pm.sendToBungeeCord("Update", "updateCache " + uuid + " " + amount);
            }
        }
    }

    public void updateMultiplier(Multiplier multiplier) {
        if (getConfig().getMessagingService().equals(MessagingService.REDIS)) { // Update using redis pub/sub
            try (Jedis jedis = redis.getPool().getResource()) {
                jedis.publish("coins-multiplier", multiplier.toJson().toString());
            }
        } else if (getConfig().getMessagingService().equals(MessagingService.BUNGEECORD)) {
            if (isBungee()) {
                ProxyServer.getInstance().getServers().keySet().forEach(server -> {
                    PluginMessageListener pml = new PluginMessageListener();
                    pml.sendToBukkit("Multiplier", Collections.singletonList(multiplier.toJson().toString()), ProxyServer.getInstance().getServerInfo(server), false);
                });
            } else if (getConfig().useBungee()) {
                PluginMessage pm = new PluginMessage();
                pm.sendToBungeeCord("Multiplier", multiplier.toJson().toString());
            }
        }
    }

    public void reloadMessages() {
        messagesMap.keySet().forEach(lang -> messagesMap.get(lang).reload());
    }
}
