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
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

/**
 *
 * @author Beelzebu
 */
public class CommandManager {

    private final CoinsCore core = CoinsCore.getInstance();
    private Command cmd;

    public void registerCommand() {
        try {
            CommandMap commandMap = (CommandMap) getPrivateField(Bukkit.getPluginManager(), "commandMap");

            String commandDescription = core.getConfig().getString("General.Command.Description", "Base command of the Coins plugin");
            String commandUsage = core.getConfig().getString("General.Command.Usage", "/coins");
            String commandPermission = core.getConfig().getString("General.Command.Permission", "coins.use");

            unregisterCommand();
            cmd = new CoinsCommand(core.getConfig().getString("General.Command.Name", "coins"), commandDescription, commandUsage, commandPermission, core.getConfig().getStringList("General.Command.Aliases"));
            commandMap.register("coins", cmd);

        } catch (SecurityException | IllegalArgumentException | NoSuchFieldException | IllegalAccessException ex) {
            core.log("An internal error has ocurred while registering the command for the plugin.");
            core.debug(ex.getCause().getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void unregisterCommand() {
        try {
            CommandMap commandMap = (CommandMap) getPrivateField(Bukkit.getPluginManager(), "commandMap");
            Map<String, Command> knownCommands = (Map<String, Command>) getPrivateField(commandMap, "knownCommands");
            knownCommands.remove(core.getConfig().getString("General.Command.Name"));
            if (cmd != null && knownCommands.get(core.getConfig().getString("General.Command.Name")) != null) {
                knownCommands.get(core.getConfig().getString("General.Command.Name")).unregister(commandMap);
            }
            core.getConfig().getStringList("General.Command.Aliases").forEach(alias -> {
                if (knownCommands.containsKey(alias)) {
                    knownCommands.remove(alias);
                    if (cmd != null && knownCommands.get(alias) != null) {
                        knownCommands.get(alias).unregister(commandMap);
                    }
                }
            });
        } catch (IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException ex) {
            core.log("An internal error has ocurred while unregistering the command for the plugin.");
            core.debug(ex.getCause().getMessage());
        }
    }

    private Object getPrivateField(Object object, String field) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Class<?> clazz = object.getClass();
        Field objectField = clazz.getDeclaredField(field);
        objectField.setAccessible(true);
        return objectField.get(object);
    }
}
