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
package io.github.beelzebu.coins.bukkit.command;

import io.github.beelzebu.coins.common.CoinsCore;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author Beelzebu
 */
public class CommandManager {

    private static final CoinsCore CORE = CoinsCore.getInstance();
    private Command cmd;

    public void registerCommand() {
        cmd = new CoinsCommand(CORE.getConfig().getString("General.Command.Name", "coins")).setDescription(CORE.getConfig().getString("General.Command.Description", "Base command of the Coins plugin")).setAliases(CORE.getConfig().getStringList("General.Command.Aliases")).setUsage(CORE.getConfig().getString("General.Command.Usage", "/coins"));
        cmd.setPermission(CORE.getConfig().getString("General.Command.Permission", "coins.use"));
        registerCommand((Plugin) CORE.getBootstrap(), cmd);
    }

    @SuppressWarnings("unchecked")
    public void unregisterCommand() {
        if (cmd != null) {
            unregisterCommand(cmd);
        }
    }

    private static void registerCommand(Plugin plugin, Command cmd) {
        unregisterCommand(cmd);
        getCommandMap().register(plugin.getName(), cmd);
    }

    private static void unregisterCommand(Command cmd) {
        Map<String, Command> knownCommands = getKnownCommandsMap();
        knownCommands.remove(cmd.getName());
        cmd.getAliases().forEach(alias -> knownCommands.remove(alias));
    }

    private static Object getPrivateField(Object object, String field) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Class<?> clazz = object.getClass();
        Field objectField = clazz.getDeclaredField(field);
        objectField.setAccessible(true);
        return objectField.get(object);
    }

    private static SimpleCommandMap getCommandMap() {
        try {
            return (SimpleCommandMap) getPrivateField(Bukkit.getPluginManager(), "commandMap");
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
            return new SimpleCommandMap(Bukkit.getServer());
        }
    }

    private static Map<String, Command> getKnownCommandsMap() {
        try {
            return (Map<String, Command>) getPrivateField(getCommandMap(), "knownCommands");
        } catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
            return new HashMap<>();
        }
    }
}
