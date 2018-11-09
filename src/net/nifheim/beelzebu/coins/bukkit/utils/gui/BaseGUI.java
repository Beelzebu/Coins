/**
 * This file is part of Coins
 *
 * Copyright (C) 2017 Beelzebu
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package net.nifheim.beelzebu.coins.bukkit.utils.gui;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.nifheim.beelzebu.coins.common.CoinsCore;
import net.nifheim.beelzebu.coins.common.utils.IConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author Beelzebu
 */
public abstract class BaseGUI {

    private static final Map<UUID, BaseGUI> inventoriesByUUID = new HashMap<>();
    private static final Map<UUID, UUID> openInventories = new HashMap<>();
    private final CoinsCore core = CoinsCore.getInstance();
    private final Inventory inv;
    private final Map<Integer, GUIAction> actions;
    private final UUID uuid;

    public BaseGUI(int size, String name) {
        inv = Bukkit.createInventory(null, size, name);
        actions = new HashMap<>();
        uuid = UUID.randomUUID();
        inventoriesByUUID.put(getUUID(), this);
    }

    public static Map<UUID, BaseGUI> getInventoriesByUUID() {
        return inventoriesByUUID;
    }

    public static Map<UUID, UUID> getOpenInventories() {
        return openInventories;
    }

    public Inventory getInv() {
        return inv;
    }

    public void setItem(int slot, ItemStack is, GUIAction action) {
        inv.setItem(slot, is);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    public void setItem(int slot, ItemStack is) {
        setItem(slot, is, null);
    }

    public void open(Player p) {
        p.closeInventory();
        p.openInventory(inv);
        openInventories.put(p.getUniqueId(), getUUID());
    }

    private UUID getUUID() {
        return uuid;
    }

    public Map<Integer, GUIAction> getActions() {
        return actions;
    }

    public void delete() {
        Bukkit.getOnlinePlayers().forEach((p) -> {
            UUID u = openInventories.get(p.getUniqueId());
            if (u.equals(getUUID())) {
                p.closeInventory();
            }
        });
        inventoriesByUUID.remove(getUUID());
    }

    public ItemStack getItem(IConfiguration config, String path) {
        Material mat;
        try {
            mat = Material.valueOf(config.getString(path + ".Material").toUpperCase());
        } catch (Exception ex) {
            core.log("The material '" + config.getString(path + ".Material").toUpperCase() + "' is invalid, it will be set as STONE.");
            mat = Material.STONE;
        }
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        config.getConfigurationSection(path).forEach(data -> {
            if (data.equals("Name")) {
                meta.setDisplayName(core.rep(config.getString(path + ".Name")));
            }
            if (data.equals("Lore")) {
                meta.setLore(core.rep(config.getStringList(path + ".Lore")));
            }
            if (data.equals("Amount")) {
                is.setAmount(config.getInt(path + ".Amount"));
            }
            if (data.equals("Damage")) {
                is.setDurability((short) config.getInt(path + ".Damage"));
            }
        });
        is.setItemMeta(meta);
        return is;
    }

    public interface GUIAction {

        void click(Player p);
    }
}
