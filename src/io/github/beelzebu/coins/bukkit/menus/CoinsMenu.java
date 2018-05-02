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
package io.github.beelzebu.coins.bukkit.menus;

import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.config.IConfiguration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 *
 * @author Beelzebu
 */
public abstract class CoinsMenu {

    protected final CoinsCore core = CoinsCore.getInstance();
    protected final Inventory inv;
    private final Map<Integer, GUIAction> actions;
    private final UUID uuid;
    private static final Map<UUID, CoinsMenu> inventoriesByUUID = new HashMap<>();
    private static final Map<UUID, UUID> openInventories = new HashMap<>();

    public CoinsMenu(int size, String name) {
        inv = Bukkit.createInventory(null, size, name);
        actions = new HashMap<>();
        uuid = UUID.randomUUID();
        inventoriesByUUID.put(getUUID(), this);
        setItems();
    }

    public final Inventory getInv() {
        return inv;
    }

    public interface GUIAction {

        void click(Player p);
    }

    public final void setItem(int slot, ItemStack is, GUIAction action) {
        inv.setItem(slot, is);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    public final void setItem(int slot, ItemStack is) {
        setItem(slot, is, null);
    }

    public final void open(Player p) {
        p.closeInventory();
        p.openInventory(inv);
        openInventories.put(p.getUniqueId(), getUUID());
    }

    private final UUID getUUID() {
        return uuid;
    }

    public static Map<UUID, CoinsMenu> getInventoriesByUUID() {
        return Collections.unmodifiableMap(inventoriesByUUID);
    }

    public static Map<UUID, UUID> getOpenInventories() {
        return Collections.unmodifiableMap(openInventories);
    }

    public final Map<Integer, GUIAction> getActions() {
        return Collections.unmodifiableMap(actions);
    }

    public final void delete() {
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

    protected abstract void setItems();
}
