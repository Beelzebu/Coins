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
package io.github.beelzebu.coins.bungee;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.bungee.config.BungeeConfig;
import io.github.beelzebu.coins.bungee.config.BungeeMessages;
import io.github.beelzebu.coins.bungee.events.CoinsChangeEvent;
import io.github.beelzebu.coins.bungee.events.MultiplierEnableEvent;
import io.github.beelzebu.coins.bungee.messaging.BungeeBungeeMessaging;
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.config.CoinsConfig;
import io.github.beelzebu.coins.common.config.MessagesConfig;
import io.github.beelzebu.coins.common.interfaces.IMethods;
import io.github.beelzebu.coins.common.messaging.BungeeMessaging;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Plugin;

/**
 *
 * @author Beelzebu
 */
public class BungeeMethods implements IMethods {

    private final Main plugin = Main.getInstance();
    private final CommandSender console = ProxyServer.getInstance().getConsole();
    private BungeeBungeeMessaging bbmessaging;
    private BungeeConfig config;

    @Override
    public Object getPlugin() {
        return plugin;
    }

    @Override
    public void loadConfig() {
        config = new BungeeConfig();

    }

    @Override
    public CoinsConfig getConfig() {
        return config == null ? config = new BungeeConfig() : config;
    }

    @Override
    public MessagesConfig getMessages(String lang) {
        return new BungeeMessages(lang);
    }

    @Override
    public void runAsync(Runnable rn) {
        ProxyServer.getInstance().getScheduler().runAsync((Plugin) getPlugin(), rn);
    }

    @Override
    public void runAsync(Runnable rn, Long timer) {
        ProxyServer.getInstance().getScheduler().schedule((Plugin) getPlugin(), rn, 0, timer / 20, TimeUnit.SECONDS);
    }

    @Override
    public void runSync(Runnable rn) {
        rn.run();
    }

    @Override
    public void executeCommand(String cmd) {
        ProxyServer.getInstance().getPluginManager().dispatchCommand(console, cmd);
    }

    @Override
    public void log(Object log) {
        console.sendMessage(CoinsCore.getInstance().rep("&8[&cCoins&8] &7" + log));
    }

    @Override
    public Object getConsole() {
        return console;
    }

    @Override
    public void sendMessage(Object commandsender, String msg) {
        ((CommandSender) commandsender).sendMessage(TextComponent.fromLegacyText(msg));
    }

    @Override
    public File getDataFolder() {
        return plugin.getDataFolder();
    }

    @Override
    public InputStream getResource(String file) {
        return plugin.getResourceAsStream(file);
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean isOnline(UUID uuid) {
        if (plugin.useRedisBungee()) {
            return RedisBungee.getApi().isPlayerOnline(uuid);
        }
        return ProxyServer.getInstance().getPlayer(uuid) != null;
    }

    @Override
    public boolean isOnline(String name) {
        if (plugin.useRedisBungee()) {
            return RedisBungee.getApi().isPlayerOnline(RedisBungee.getApi().getUuidFromName(name));
        }
        return ProxyServer.getInstance().getPlayer(name) != null;
    }

    @Override
    public UUID getUUID(String name) {
        if (plugin.useRedisBungee()) {
            return RedisBungee.getApi().getUuidFromName(name);
        }
        return ProxyServer.getInstance().getPlayer(name) != null ? ProxyServer.getInstance().getPlayer(name).getUniqueId() : null;
    }

    @Override
    public String getName(UUID uuid) {
        if (plugin.useRedisBungee()) {
            return RedisBungee.getApi().getNameFromUuid(uuid);
        }
        return ProxyServer.getInstance().getPlayer(uuid) != null ? ProxyServer.getInstance().getPlayer(uuid).getName() : null;
    }

    @Override
    public void callCoinsChangeEvent(UUID uuid, double oldCoins, double newCoins) {
        ProxyServer.getInstance().getPluginManager().callEvent(new CoinsChangeEvent(uuid, oldCoins, newCoins));
    }

    @Override
    public void callMultiplierEnableEvent(Multiplier multiplier) {
        ProxyServer.getInstance().getPluginManager().callEvent(new MultiplierEnableEvent(multiplier));
    }

    @Override
    public List<String> getPermissions(UUID uuid) {
        List<String> permissions = new ArrayList<>();
        if (isOnline(uuid)) {
            permissions.addAll(ProxyServer.getInstance().getPlayer(uuid).getPermissions());
        }
        return permissions;
    }

    @Override
    public Logger getLogger() {
        return plugin.getLogger();
    }

    @Override
    public BungeeMessaging getBungeeMessaging() {
        return bbmessaging == null ? bbmessaging = new BungeeBungeeMessaging() : bbmessaging;
    }
}
