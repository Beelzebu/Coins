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
package io.github.beelzebu.coins.common.plugin;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.api.Multiplier;
import io.github.beelzebu.coins.api.cache.CacheProvider;
import io.github.beelzebu.coins.api.config.AbstractConfigFile;
import io.github.beelzebu.coins.api.config.CoinsConfig;
import io.github.beelzebu.coins.api.dependency.Dependency;
import io.github.beelzebu.coins.api.dependency.DependencyManager;
import io.github.beelzebu.coins.api.dependency.DependencyRegistry;
import io.github.beelzebu.coins.api.executor.Executor;
import io.github.beelzebu.coins.api.executor.ExecutorManager;
import io.github.beelzebu.coins.api.messaging.AbstractMessagingService;
import io.github.beelzebu.coins.api.messaging.MessagingService;
import io.github.beelzebu.coins.api.plugin.CoinsBootstrap;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import io.github.beelzebu.coins.api.storage.StorageProvider;
import io.github.beelzebu.coins.api.storage.StorageType;
import io.github.beelzebu.coins.api.utils.StringUtils;
import io.github.beelzebu.coins.common.cache.LocalCache;
import io.github.beelzebu.coins.common.cache.RedisCache;
import io.github.beelzebu.coins.common.messaging.DummyMessaging;
import io.github.beelzebu.coins.common.messaging.RedisMessaging;
import io.github.beelzebu.coins.common.storage.MySQL;
import io.github.beelzebu.coins.common.storage.SQLite;
import io.github.beelzebu.coins.common.utils.FileManager;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author Beelzebu
 */
@Getter
@RequiredArgsConstructor
public abstract class CommonCoinsPlugin implements CoinsPlugin {

    private final FileManager fileManager;
    private final CoinsBootstrap bootstrap;
    @Getter(AccessLevel.NONE)
    private final HashMap<String, AbstractConfigFile> messagesMap = new HashMap<>();
    private final Gson gson = new Gson();
    private final DependencyManager dependencyManager = new DependencyManager(this, new DependencyRegistry(this));
    protected AbstractMessagingService messagingService;
    protected boolean logEnabled = false;
    protected StorageType storageType;
    private StorageProvider database;
    private CacheProvider cache;

    public CommonCoinsPlugin(CoinsBootstrap bootstrap) {
        CoinsAPI.setPlugin(this);
        this.bootstrap = bootstrap;
        fileManager = new FileManager(this);
    }

    @Override
    public void load() {
        fileManager.copyFiles();
    }

    @Override
    public void enable() { // now the plugin is enabled and we can read config files
        Arrays.asList(Objects.requireNonNull(fileManager.getMessagesFolder().listFiles())).forEach(file -> messagesMap.put((file.getName().split("_").length == 2 ? file.getName().split("_")[1] : "default").split(".yml")[0], bootstrap.getFileAsConfig(file)));
        // update files before we read something
        fileManager.updateFiles();
        logEnabled = getConfig().isDebugFile();
        // identify storage type and start messaging service before start things
        storageType = getConfig().getStorageType();
        getDependencyManager().loadStorageDependencies(storageType);
        if (getConfig().getString("Messaging Service").equalsIgnoreCase(MessagingService.BUNGEECORD.toString())) {
            messagingService = bootstrap.getBungeeMessaging();
        } else if (getConfig().getString("Messaging Service").equalsIgnoreCase(MessagingService.REDIS.toString())) {
            messagingService = new RedisMessaging();
        } else {
            messagingService = new DummyMessaging();
        }
        if (messagingService.getType() != MessagingService.REDIS) {
            getDependencyManager().loadDependencies(EnumSet.of(Dependency.CAFFEINE));
        }
        try {
            if (!getMultipliersFile().exists()) {
                getMultipliersFile().createNewFile();
            }
            Iterator<String> lines = Files.readAllLines(getMultipliersFile().toPath()).iterator();
            while (lines.hasNext()) {
                String line = lines.next();
                try {
                    getCache().addMultiplier(Multiplier.fromJson(line));
                } catch (JsonParseException ignore) { // Invalid line
                    debug(line + " isn't a valid multiplier in json format.");
                    lines.remove();
                }
            }
            Files.write(getMultipliersFile().toPath(), Lists.newArrayList(lines));
        } catch (IOException ex) {
            log("An error has occurred loading multipliers from local storage.");
            debug(ex.getMessage());
        }
        if (storageType.equals(StorageType.SQLITE) && getConfig().getInt("Database Version", 1) < 2) {
            try {
                Files.move(new File(bootstrap.getDataFolder(), "database.db").toPath(), new File(bootstrap.getDataFolder(), "database.old.db").toPath());
            } catch (IOException ex) {
                log("An error has occurred moving the old database");
                debug(ex.getMessage());
            }
        }
        getDatabase().setup();
        messagingService.start();
        motd(true);
        getMessagingService().getMultipliers();
        getMessagingService().getExecutors();
        loadExecutors();
    }

    @Override
    public void disable() {
        getDatabase().shutdown();
        messagingService.stop();
        motd(false);
    }

    @Override
    public final void loadExecutors() {
        getConfig().getConfigurationSection("Command executor").forEach(id -> ExecutorManager.addExecutor(new Executor(id, getConfig().getString("Command executor." + id + ".Displayname", id), getConfig().getDouble("Command executor." + id + ".Cost", 0), getConfig().getStringList("Command executor." + id + ".Command"))));
    }

    @Override
    public final File getMultipliersFile() {
        return new File(bootstrap.getDataFolder(), "multipliers.json");
    }

    @Override
    public final StorageProvider getDatabase() {
        if (database != null) {
            return database;
        }
        switch (storageType) {
            case MARIADB:
            case MYSQL:
                return database = new MySQL(bootstrap.getPlugin());
            case SQLITE:
                return database = new SQLite(bootstrap.getPlugin());
            default:
                return null;
        }
    }

    @Override
    public final CacheProvider getCache() {
        return cache == null ? cache = messagingService.getType().equals(MessagingService.REDIS) ? new RedisCache() : new LocalCache() : cache;
    }

    @Override
    public final CoinsConfig getConfig() {
        return bootstrap.getPluginConfig();
    }

    @Override
    public final AbstractConfigFile getMessages(String locale) {
        return Optional.ofNullable(messagesMap.get(locale.split("_")[0])).orElse(messagesMap.get("default"));
    }

    @Override
    public final String getString(String path, String locale) {
        try {
            return StringUtils.rep(getMessages(locale).getString(path));
        } catch (NullPointerException ex) {
            bootstrap.log("The string " + path + " does not exists in the messages_" + locale.split("_")[0] + ".yml file.");
            debug(ex);
            return StringUtils.rep(getMessages("").getString(path, ""));
        }
    }

    @Override
    public final void reloadMessages() {
        messagesMap.keySet().forEach(lang -> messagesMap.get(lang).reload());
    }

    @Override
    public final void log(String message) {
        bootstrap.log(message);
        logToFile(message);
    }

    @Override
    public final void debug(String message) {
        if (getConfig().isDebug()) {
            bootstrap.sendMessage(bootstrap.getConsole(), StringUtils.rep("&8[&cCoins&8] &cDebug: &7" + message));
        }
        logToFile(message);
    }

    @Override
    public final void debug(Exception ex) {
        debug("Unknown Exception:");
        debug("   Error message: " + ex.getMessage());
        debug("   Stacktrace: " + getStackTrace(ex));
    }

    @Override
    public final void debug(SQLException ex) {
        debug("SQLException:");
        debug("   Database state: " + ex.getSQLState());
        debug("   Error code: " + ex.getErrorCode());
        debug("   Error message: " + ex.getMessage());
        debug("   Stacktrace: " + getStackTrace(ex));
    }

    @Override
    public final UUID getUniqueId(String name, boolean fromdb) {
        if (!fromdb && bootstrap.getUUID(name) != null) {
            return bootstrap.getUUID(name);
        }
        return getDatabase().getUUID(name.toLowerCase());
    }

    @Override
    public final String getName(UUID uniqueId, boolean fromdb) {
        if (!fromdb && bootstrap.getName(uniqueId) != null) {
            return bootstrap.getName(uniqueId);
        }
        return getDatabase().getName(uniqueId);
    }

    private void motd(boolean enable) {
        bootstrap.sendMessage(bootstrap.getConsole(), StringUtils.rep(""));
        bootstrap.sendMessage(bootstrap.getConsole(), StringUtils.rep("&6-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"));
        bootstrap.sendMessage(bootstrap.getConsole(), StringUtils.rep("           &4Coins &fBy:  &7Beelzebu"));
        bootstrap.sendMessage(bootstrap.getConsole(), StringUtils.rep(""));
        StringBuilder version = new StringBuilder();
        int spaces = (42 - ("v: " + bootstrap.getVersion()).length()) / 2;
        for (int i = 0; i < spaces; i++) {
            version.append(" ");
        }
        version.append(StringUtils.rep("&4v: &f" + bootstrap.getVersion()));
        bootstrap.sendMessage(bootstrap.getConsole(), version.toString());
        bootstrap.sendMessage(bootstrap.getConsole(), StringUtils.rep("&6-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-"));
        bootstrap.sendMessage(bootstrap.getConsole(), StringUtils.rep(""));
        // Only send this in the onEnable
        if (enable) {
            logToFile("Enabled Coins v: " + bootstrap.getVersion());
            debug("Debug mode is enabled.");
            if (!logEnabled) {
                debug("Logging to file is disabled, all debug messages will be sent to the console.");
            }
            debug("Using \"" + storageType.toString().toLowerCase() + "\" for storage.");
            if (!messagingService.getType().equals(MessagingService.NONE)) {
                debug("Using \"" + messagingService.getType().toString().toLowerCase() + "\" as messaging service.");
            }
            debug("Using \"" + getCache().getClass().getSimpleName() + "\" for cache");
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

    private String getStackTrace(Exception ex) {
        try (StringWriter stringWriter = new StringWriter(); PrintWriter printWriter = new PrintWriter(stringWriter)) {
            ex.printStackTrace(printWriter);
            return stringWriter.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Error getting the stacktrace";
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
                Logger.getLogger(CoinsPlugin.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(log, true))) {
            writer.write("[" + sdf.format(System.currentTimeMillis()) + "] " + StringUtils.removeColor(msg.toString()));
            writer.newLine();
        } catch (IOException ex) {
            Logger.getLogger(CoinsPlugin.class.getName()).log(Level.WARNING, "Can''t save the debug to the file", ex);
        }
    }

    private String getFromURL(String surl) {
        String response = null;
        try {
            URL url = new URL(surl);
            try (Scanner s = new Scanner(url.openStream())) {
                if (s.hasNext()) {
                    response = s.next();
                }
            }
        } catch (IOException ex) {
            debug("Failed to connect to URL: " + surl);
        }
        return response;
    }
}
