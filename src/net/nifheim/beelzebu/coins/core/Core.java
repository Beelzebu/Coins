/**
 * This file is part of Coins
 *
 * Copyright (C) 2017 Beelzebu
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
import net.nifheim.beelzebu.coins.core.executor.ExecutorManager;
import net.nifheim.beelzebu.coins.core.multiplier.Multiplier;
import net.nifheim.beelzebu.coins.core.multiplier.MultiplierData;
import net.nifheim.beelzebu.coins.core.utils.CacheManager;
import net.nifheim.beelzebu.coins.core.utils.CoinsConfig;
import net.nifheim.beelzebu.coins.core.utils.FileManager;
import net.nifheim.beelzebu.coins.core.utils.IMethods;
import net.nifheim.beelzebu.coins.core.utils.MessagesManager;
import net.nifheim.beelzebu.coins.database.*;

/**
 *
 * @author Beelzebu
 */
public class Core {

    private static Core instance;
    private IMethods mi;
    private FileManager fileUpdater;
    private CoinsDatabase db;
    @Getter
    private StorageType storageType;
    private ExecutorManager executorManager;
    private HashMap<String, MessagesManager> messagesMap;

    public static Core getInstance() {
        return instance == null ? instance = new Core() : instance;
    }

    public void setup(IMethods methodinterface) {
        mi = methodinterface;
        fileUpdater = new FileManager(this);
        fileUpdater.copyFiles();
        messagesMap = new HashMap<>();
    }

    public void shutdown() {
        motd(false);
    }

    public void start() {
        fileUpdater.updateFiles();
        try {
            storageType = StorageType.valueOf(getConfig().getString("Storage Type", "SQLITE"));
        } catch (Exception ex) { // invalid storage type
            storageType = StorageType.SQLITE;
            log("Invalid Storage Type selected in the config, possible values: " + Arrays.toString(StorageType.values()));
        }
        if (!storageType.equals(StorageType.SQLITE) && isBungee()) {
            log(" ");
            log("    WARNING");
            log(" ");
            log("Bungeecord doesn't support SQLite storage, change it to MySQL and reload the plugin.");
            log(" ");
            log("    WARNING");
            log(" ");
            return;
        }
        motd(true);
        getDatabase();
        executorManager = new ExecutorManager();
    }

    private void motd(boolean enable) {
        mi.sendMessage(mi.getConsole(), rep(""));
        mi.sendMessage(mi.getConsole(), rep("&6-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"));
        mi.sendMessage(mi.getConsole(), rep("           &4Coins &fBy:  &7Beelzebu"));
        mi.sendMessage(mi.getConsole(), rep(""));
        String version = "";
        int spaces = (42 - ("v: " + mi.getVersion()).length()) / 2;
        for (int i = 0; i < spaces; i++) {
            version += " ";
        }
        version += rep("&4v: &f" + mi.getVersion());
        mi.sendMessage(mi.getConsole(), version);
        mi.sendMessage(mi.getConsole(), rep("&6-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"));
        mi.sendMessage(mi.getConsole(), rep(""));
        // Only send this in the onEnable
        if (enable) {
            if (getConfig().getBoolean("Debug", false)) {
                log("Debug mode is enabled.");
            }
            log("Using " + storageType + " for storage.");
        }
    }

    public IMethods getMethods() {
        return mi;
    }

    public void debug(Object msg) {
        if (getConfig().getBoolean("Debug")) {
            mi.sendMessage(mi.getConsole(), (rep("&8[&cCoins&8] &cDebug: &7" + msg)));
        }
        logToFile(msg);
    }

    public void debug(SQLException ex) {
        debug("SQLException: ");
        debug("   Database state: " + ex.getSQLState());
        debug("   Error code: " + ex.getErrorCode());
        debug("   Error message: " + ex.getMessage());
    }

    public void log(Object msg) {
        mi.log(msg);
        logToFile(msg);
    }

    private void logToFile(Object msg) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        File log = new File(getDataFolder(), "/logs/latest.log");
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

    public Boolean isOnline(UUID uuid) {
        return mi.isOnline(uuid);
    }

    public String getNick(UUID uuid) {
        return mi.getName(uuid) != null ? mi.getName(uuid) : getDatabase().getNick(uuid);
    }

    public UUID getUUID(String player) {
        return mi.getUUID(player) != null ? mi.getUUID(player) : getDatabase().getUUID(player);
    }

    public CoinsDatabase getDatabase() {
        switch (storageType) {
            case MYSQL:
                return db == null ? db = new NewMySQL() : db;
            case SQLITE:
                return db == null ? db = new NewSQLite() : db;
            case REDIS:
                return db == null ? db = new NewRedis() : db;
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

    public String rep(String msg, MultiplierData multiplierData) {
        String string = msg;
        if (multiplierData != null) {
            string = msg
                    .replaceAll("%enabler%", multiplierData.getEnabler())
                    .replaceAll("%server%", multiplierData.getServer())
                    .replaceAll("%amount%", String.valueOf(multiplierData.getAmount()))
                    .replaceAll("%minutes%", String.valueOf(multiplierData.getMinutes()))
                    .replaceAll("%id%", String.valueOf(multiplierData.getID()));
        }
        return rep(string);
    }

    public List<String> rep(List<String> msgs) {
        List<String> message = new ArrayList<>();
        msgs.forEach(msg -> {
            message.add(rep(msg));
        });
        return message;
    }

    public List<String> rep(List<String> msgs, MultiplierData multiplierData) {
        List<String> message = new ArrayList<>();
        msgs.forEach(msg -> {
            message.add(rep(msg, multiplierData));
        });
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
            messagesMap.put(lang, mi.getMessages(lang));
            return mi.getMessages(lang);
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

    public void updateCache(UUID player, Double coins) {
        CacheManager.updateCoins(player, coins);
        if (isBungee()) {
            ProxyServer.getInstance().getServers().keySet().forEach(server -> {
                PluginMessageListener pml = new PluginMessageListener();
                pml.sendToBukkit("Update", Arrays.asList(player + " " + coins), ProxyServer.getInstance().getServerInfo(server), false);
            });
        } else if (getConfig().useBungee()) {
            PluginMessage pm = new PluginMessage();
            pm.sendToBungeeCord("Update", "updateCache " + player + " " + coins);
        }
    }

    public void updateMultiplier(Multiplier multiplier) {
        CacheManager.addMultiplier(multiplier.getServer(), multiplier);
        List<String> message = new ArrayList<>();
        message.add(multiplier.getServer());
        message.add(String.valueOf(multiplier.isEnabled()));
        message.add(multiplier.getEnabler());
        message.add(String.valueOf(multiplier.getAmount()));
        message.add(String.valueOf(System.currentTimeMillis() + multiplier.checkTime()));
        if (isBungee()) {
            ProxyServer.getInstance().getServers().keySet().forEach(server -> {
                PluginMessageListener pml = new PluginMessageListener();
                pml.sendToBukkit("Multiplier", message, ProxyServer.getInstance().getServerInfo(server), false);
            });
        } else if (getConfig().useBungee()) {
            PluginMessage pm = new PluginMessage();
            pm.sendToBungeeCord("Multiplier", message);
        }
    }

    public void reloadMessages() {
        messagesMap.keySet().forEach((lang) -> {
            messagesMap.get(lang).reload();
        });
    }
}
