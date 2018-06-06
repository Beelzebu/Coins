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
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.common.config.CoinsConfig;
import io.github.beelzebu.coins.common.config.MessagesConfig;
import io.github.beelzebu.coins.common.database.CoinsDatabase;
import io.github.beelzebu.coins.common.database.MySQL;
import io.github.beelzebu.coins.common.database.SQLite;
import io.github.beelzebu.coins.common.database.StorageType;
import io.github.beelzebu.coins.common.dependency.Dependency;
import io.github.beelzebu.coins.common.messaging.DummyMessaging;
import io.github.beelzebu.coins.common.messaging.IMessagingService;
import io.github.beelzebu.coins.common.messaging.MessagingService;
import io.github.beelzebu.coins.common.messaging.RedisMessaging;
import io.github.beelzebu.coins.common.plugin.CoinsBootstrap;
import io.github.beelzebu.coins.common.utils.FileManager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Beelzebu
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CoinsCore {

    @Getter(AccessLevel.NONE)
    private static CoinsCore instance;
    @Getter(AccessLevel.NONE)
    private CoinsDatabase db;
    private CoinsBootstrap bootstrap;
    private StorageType storageType;
    private IMessagingService messagingService;
    private boolean logEnabled = false;
    @Getter(AccessLevel.NONE)
    private final HashMap<String, MessagesConfig> messagesMap = new HashMap<>();
    private final Gson gson = new Gson();

    public static CoinsCore getInstance() {
        return instance == null ? instance = new CoinsCore() : instance;
    }

    public void setup(CoinsBootstrap bootstrap) { // the plugin was loaded, so copy files and download dependencies
        this.bootstrap = bootstrap;
        EnumSet<Dependency> dependencies = EnumSet.of(Dependency.CAFFEINE, Dependency.COMMONS_IO);
        log("Loading main dependencies: " + dependencies);
        bootstrap.getPlugin().getDependencyManager().loadDependencies(dependencies);
        new FileManager().copyFiles();
    }

    public void start() { // now the plugin is enabled and we can read config files
        logEnabled = true;
        // update config files to avoid outdated config options
        new FileManager().updateFiles();
        // identify storage type and start messaging service before start things
        storageType = getConfig().getStorageType();
        bootstrap.getPlugin().getDependencyManager().loadStorageDependencies(storageType);
        if (getConfig().getString("Messaging Service").equalsIgnoreCase(MessagingService.BUNGEECORD.toString())) {
            messagingService = bootstrap.getBungeeMessaging();
        } else if (getConfig().getString("Messaging Service").equalsIgnoreCase(MessagingService.REDIS.toString())) {
            messagingService = new RedisMessaging();
        } else {
            messagingService = new DummyMessaging();
        }
        bootstrap.getPlugin().enable(); // do stuff
        try {
            if (!CacheManager.MULTIPLIERS_FILE.exists()) {
                CacheManager.MULTIPLIERS_FILE.createNewFile();
            }
            Iterator<String> lines = FileUtils.readLines(CacheManager.MULTIPLIERS_FILE, Charsets.UTF_8).iterator();
            while (lines.hasNext()) {
                String line = lines.next();
                try {
                    Multiplier multiplier = Multiplier.fromJson(line, false);
                    CacheManager.getMultipliersData().put(multiplier.getServer().toLowerCase(), multiplier);
                } catch (JsonParseException ignore) { // Invalid line
                    debug(line + " isn't a valid multiplier in json format.");
                    lines.remove();
                    FileUtils.writeLines(CacheManager.MULTIPLIERS_FILE, Lists.newArrayList(lines));
                }
            }
        } catch (IOException ex) {
            log("An error has ocurred loading multipliers from local storage.");
            debug(ex.getMessage());
        }
        if (storageType.equals(StorageType.SQLITE) && getConfig().getInt("Database Version", 1) < 2) {
            try {
                Files.move(new File(bootstrap.getDataFolder(), "database.db").toPath(), new File(bootstrap.getDataFolder(), "database.old.db").toPath());
            } catch (IOException ex) {
                log("An error has ocurred moving the old database");
                debug(ex.getMessage());
            }
        }
        getDatabase().setup();
        if (!messagingService.getType().equals(MessagingService.NONE)) {
            messagingService.start();
        }
        motd(true);
        getMessagingService().getMultipliers();
        getMessagingService().getExecutors();
    }

    public void shutdown() {
        db.shutdown();
        messagingService.stop();
        bootstrap.getPlugin().disable();
        motd(false);
    }

    private void motd(boolean enable) {
        bootstrap.sendMessage(bootstrap.getConsole(), rep(""));
        bootstrap.sendMessage(bootstrap.getConsole(), rep("&6-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"));
        bootstrap.sendMessage(bootstrap.getConsole(), rep("           &4Coins &fBy:  &7Beelzebu"));
        bootstrap.sendMessage(bootstrap.getConsole(), rep(""));
        StringBuilder version = new StringBuilder();
        int spaces = (42 - ("v: " + bootstrap.getVersion()).length()) / 2;
        for (int i = 0; i < spaces; i++) {
            version.append(" ");
        }
        version.append(rep("&4v: &f" + bootstrap.getVersion()));
        bootstrap.sendMessage(bootstrap.getConsole(), version.toString());
        bootstrap.sendMessage(bootstrap.getConsole(), rep("&6-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"));
        bootstrap.sendMessage(bootstrap.getConsole(), rep(""));
        // Only send this in the onEnable
        if (enable) {
            logToFile("Enabled Coins v: " + bootstrap.getVersion());
            if (getConfig().getBoolean("Debug", false)) {
                log("Debug mode is enabled.");
            }
            if (!logEnabled) {
                log("Logging to file is disabled, all debug messages will be sent to the console.");
            }
            log("Using \"" + storageType.toString().toLowerCase() + "\" for storage.");
            if (!messagingService.getType().equals(MessagingService.NONE)) {
                log("Using \"" + messagingService.getType().toString().toLowerCase() + "\" as messaging service.");
            }
            bootstrap.runAsync(() -> { // run update check async, so it doesn't delay the startup
                String upt = "You have the newest version";
                String response = getFromURL("https://api.spigotmc.org/legacy/update.php?resource=48536");
                if (response == null) {
                    upt = "Failed to check for updates :(";
                } else if (!response.equalsIgnoreCase(bootstrap.getVersion())) {
                    upt = "There is a new version available! [" + response + "]";
                }
                log(upt);
            });
        }
    }

    public void debug(Object msg) {
        if (!logEnabled || getConfig().getBoolean("Debug")) {
            bootstrap.sendMessage(bootstrap.getConsole(), rep("&8[&cCoins&8] &cDebug: &7" + msg));
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
        bootstrap.log(msg);
        logToFile(msg);
    }

    private void logToFile(Object msg) {
        if (!logEnabled) {
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        File log = new File(bootstrap.getDataFolder(), "/logs/latest.log");
        if (!log.exists()) {
            try {
                log.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(CoinsCore.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(log, true))) {
            writer.write("[" + sdf.format(System.currentTimeMillis()) + "] " + removeColor(msg.toString()));
            writer.newLine();
        } catch (IOException ex) {
            Logger.getLogger(CoinsCore.class.getName()).log(Level.WARNING, "Can''t save the debug to the file", ex);
        }
    }

    public String getNick(UUID uuid, boolean fromdb) {
        if (!fromdb && bootstrap.getName(uuid) != null) {
            return bootstrap.getName(uuid);
        }
        return getDatabase().getNick(uuid);
    }

    public UUID getUUID(String name, boolean fromdb) {
        if (!fromdb && bootstrap.getUUID(name) != null) {
            return bootstrap.getUUID(name);
        }
        return getDatabase().getUUID(name.toLowerCase());
    }

    public CoinsDatabase getDatabase() {
        if (db != null) {
            return db;
        }
        switch (storageType) {
            case MARIADB:
            case MYSQL:
                return db = new MySQL();
            case SQLITE:
                return db = new SQLite();
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
            string = msg.replaceAll("%enabler%", multiplier.getEnablerName()).replaceAll("%server%", multiplier.getServer()).replaceAll("%amount%", String.valueOf(multiplier.getAmount())).replaceAll("%minutes%", String.valueOf(multiplier.getMinutes())).replaceAll("%id%", String.valueOf(multiplier.getId()));
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
        return bootstrap.getPluginConfig();
    }

    public MessagesConfig getMessages(String locale) {
        String lang = locale;
        if (lang == null || lang.equals("")) {
            lang = "default";
        }
        lang = lang.split("_")[0];
        if (!messagesMap.containsKey(lang)) {
            if (new File(bootstrap.getDataFolder() + "/messages", "messages_" + lang + ".yml").exists()) {
                messagesMap.put(lang, bootstrap.getMessages("_" + lang));
            } else {
                messagesMap.put(lang, bootstrap.getMessages(""));
            }
        }
        return messagesMap.get(lang);
    }

    public String getString(String path, String locale) {
        try {
            return rep(getMessages(locale).getString(path));
        } catch (NullPointerException ex) {
            bootstrap.log("The string " + path + " does not exists in the messages_" + locale.split("_")[0] + ".yml file.");
            debug(ex);
            return rep(getMessages("").getString(path, ""));
        }
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
        } catch (IOException ex) {
            debug("Failed to connect to URL: " + surl);
        }
        return response;
    }
}
