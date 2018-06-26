/**
 * This file is part of Coins
 *
 * Copyright (C) 2018 Beelzebu
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
package io.github.beelzebu.coins.api.plugin;

import com.google.gson.Gson;
import io.github.beelzebu.coins.api.Multiplier;
import io.github.beelzebu.coins.api.cache.CacheProvider;
import io.github.beelzebu.coins.api.config.AbstractConfigFile;
import io.github.beelzebu.coins.api.config.CoinsConfig;
import io.github.beelzebu.coins.api.dependency.Dependency;
import io.github.beelzebu.coins.api.dependency.DependencyManager;
import io.github.beelzebu.coins.api.executor.Executor;
import io.github.beelzebu.coins.api.executor.ExecutorManager;
import io.github.beelzebu.coins.api.messaging.AbstractMessagingService;
import io.github.beelzebu.coins.api.messaging.MessagingService;
import io.github.beelzebu.coins.api.storage.StorageProvider;
import io.github.beelzebu.coins.api.storage.StorageType;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;

/**
 *
 * @author Beelzebu
 */
@Getter
@RequiredArgsConstructor
public abstract class CoinsPlugin {

    @Getter
    @Setter
    private static CoinsPlugin instance;
    private final CoinsBootstrap bootstrap;
    @Getter(AccessLevel.NONE)
    private final HashMap<String, AbstractConfigFile> messagesMap = new HashMap<>();
    private final Gson gson = new Gson();
    private final DependencyManager dependencyManager = new DependencyManager();
    protected AbstractMessagingService messagingService;
    protected boolean logEnabled = false;
    protected StorageType storageType;

    public void load() {
        EnumSet<Dependency> dependencies = EnumSet.of(Dependency.CAFFEINE);
        log("Loading main dependencies: " + dependencies);
        getDependencyManager().loadDependencies(dependencies);
    }

    public abstract void enable();

    public void disable() {
        getDatabase().shutdown();
        messagingService.stop();
        motd(false);
    }

    public abstract CacheProvider getCache();

    public final void loadExecutors() {
        getConfig().getConfigurationSection("Command executor").forEach(id -> ExecutorManager.addExecutor(new Executor(id, getConfig().getString("Command executor." + id + ".Displayname", id), getConfig().getDouble("Command executor." + id + ".Cost", 0), getConfig().getStringList("Command executor." + id + ".Command"))));
    }

    public final File getMultipliersFile() {
        return new File(bootstrap.getDataFolder(), "multipliers.json");
    }

    protected final void motd(boolean enable) {
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
            if (getConfig().isDebug()) {
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

    public final void debug(String msg) {
        if (getConfig().isDebug()) {
            bootstrap.sendMessage(bootstrap.getConsole(), rep("&8[&cCoins&8] &cDebug: &7" + msg));
        }
        logToFile(msg);
    }

    public final void debug(SQLException ex) {
        debug("SQLException:");
        debug("   Database state: " + ex.getSQLState());
        debug("   Error code: " + ex.getErrorCode());
        debug("   Error message: " + ex.getMessage());
    }

    public final void debug(Exception ex) {
        debug("Unknown Exception:");
        debug("   Error message: " + ex.getMessage());
        debug(getStackTrace(ex));
    }

    public final void log(String msg) {
        bootstrap.log(msg);
        logToFile(msg);
    }

    private final void logToFile(Object msg) {
        if (!logEnabled) {
            return;
        }
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        File log = new File(bootstrap.getDataFolder(), "/logs/latest.log");
        if (!log.exists()) {
            try {
                log.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(CoinsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(log, true))) {
            writer.write("[" + sdf.format(System.currentTimeMillis()) + "] " + removeColor(msg.toString()));
            writer.newLine();
        } catch (IOException ex) {
            Logger.getLogger(CoinsPlugin.class.getName()).log(Level.WARNING, "Can''t save the debug to the file", ex);
        }
    }

    private final String getStackTrace(Exception ex) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        ex.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    public final String getName(UUID uuid, boolean fromdb) {
        if (!fromdb && bootstrap.getName(uuid) != null) {
            return bootstrap.getName(uuid);
        }
        return getDatabase().getName(uuid);
    }

    public final UUID getUUID(String name, boolean fromdb) {
        if (!fromdb && bootstrap.getUUID(name) != null) {
            return bootstrap.getUUID(name);
        }
        return getDatabase().getUUID(name.toLowerCase());
    }

    public abstract StorageProvider getDatabase();

    public final String rep(String msg) {
        if (msg == null) {
            return "";
        }
        String message = msg;
        if (getConfig() != null) {
            message = message.replaceAll("%prefix%", getConfig().getString("Prefix", "&c&lCoins &6&l>&7"));
        }
        return message.replaceAll("&", "§");
    }

    public final String rep(String msg, Multiplier multiplier) {
        String string = msg;
        if (multiplier != null) {
            string = msg.replaceAll("%enabler%", multiplier.getEnablerName()).replaceAll("%server%", multiplier.getServer()).replaceAll("%amount%", String.valueOf(multiplier.getAmount())).replaceAll("%minutes%", String.valueOf(multiplier.getMinutes())).replaceAll("%id%", String.valueOf(multiplier.getId()));
        }
        return rep(string);
    }

    public final List<String> rep(List<String> msgs) {
        List<String> message = new ArrayList<>();
        msgs.forEach(msg -> message.add(rep(msg)));
        return message;
    }

    public final List<String> rep(List<String> msgs, Multiplier multiplierData) {
        List<String> message = new ArrayList<>();
        msgs.forEach(msg -> message.add(rep(msg, multiplierData)));
        return message;
    }

    public final String removeColor(String str) {
        return ChatColor.stripColor(rep(str)).replaceAll("Debug: ", "");
    }

    public final CoinsConfig getConfig() {
        return bootstrap.getPluginConfig();
    }

    public final AbstractConfigFile getMessages(String locale) {
        String lang = "_" + locale.split("_")[0];
        File file = new File(bootstrap.getDataFolder(), "messages" + lang + ".yml");
        if (lang == null || lang.equals("default") || !file.exists()) {
            lang = "";
        }
        file = new File(bootstrap.getDataFolder(), "messages" + lang + ".yml");
        if (!messagesMap.containsKey(lang)) {
            messagesMap.put(lang, bootstrap.getFileAsConfig(file));
        }
        return messagesMap.get(lang);
    }

    public final String getString(String path, String locale) {
        try {
            return rep(getMessages(locale).getString(path));
        } catch (NullPointerException ex) {
            bootstrap.log("The string " + path + " does not exists in the messages_" + locale.split("_")[0] + ".yml file.");
            debug(ex);
            return rep(getMessages("").getString(path, ""));
        }
    }

    public final void reloadMessages() {
        messagesMap.keySet().forEach(lang -> messagesMap.get(lang).reload());
    }

    private final String getFromURL(String surl) {
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
