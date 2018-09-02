/*
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

import com.google.common.base.Preconditions;
import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.api.config.AbstractConfigFile;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import io.github.beelzebu.coins.api.utils.StringUtils;
import io.github.beelzebu.coins.bukkit.utils.CompatUtils;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

/**
 * @author Beelzebu
 */
public abstract class CoinsMenu {

    private static final Map<UUID, CoinsMenu> inventoriesByUUID = new HashMap<>();
    private static final Map<UUID, UUID> openInventories = new HashMap<>();
    protected final CoinsPlugin plugin = CoinsAPI.getPlugin();
    protected final AbstractConfigFile multipliersConfig = plugin.getBootstrap().getFileAsConfig(new File(plugin.getBootstrap().getDataFolder(), "multipliers.yml"));
    protected final Inventory inv;
    protected final UUID uuid;
    private final Map<Integer, GUIAction> actions;

    public CoinsMenu(int size, String name) {
        if (size % 9 != 0 && (size * 9) % 9 != 0) {
            plugin.log("Menu size must be a multiple of 9, " + size + " isn't.");
            size = 54;
        }
        inv = Bukkit.createInventory(null, size < 9 ? size * 9 : size, name != null && !"".equals(name) ? name : new TranslatableComponent("tile.chest.name").toLegacyText());
        actions = new HashMap<>();
        uuid = UUID.randomUUID();
        inventoriesByUUID.put(uuid, this);
    }

    public static Map<UUID, CoinsMenu> getInventoriesByUUID() {
        return Collections.unmodifiableMap(inventoriesByUUID);
    }

    public static Map<UUID, UUID> getOpenInventories() {
        return openInventories;
    }

    public final Inventory getInv() {
        return inv;
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

    public void open(Player p) {
        plugin.getBootstrap().runSync(() -> {
            p.closeInventory();
            if (inv.getContents().length == 0) {
                setItems();
            }
            p.openInventory(inv);
            openInventories.put(p.getUniqueId(), uuid);
        });
    }

    public final Map<Integer, GUIAction> getActions() {
        return Collections.unmodifiableMap(actions);
    }

    public final void delete() {
        Bukkit.getOnlinePlayers().stream().filter(p -> openInventories.get(p.getUniqueId()) != null && openInventories.get(p.getUniqueId()).equals(uuid)).forEach(Player::closeInventory);
        inventoriesByUUID.remove(uuid);
    }

    public ItemStack getItem(AbstractConfigFile config, String path) {
        return getItem(config, path, null);
    }

    public ItemStack getItem(AbstractConfigFile config, String path, Player player) {
        Preconditions.checkNotNull(config, "Config can't be null");
        Preconditions.checkNotNull(path, "Item path can't be null");
        Material mat;
        try {
            mat = Material.valueOf(config.getString(path + ".Material").toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.log("\"" + config.getString(path + ".Material").toUpperCase() + "\" is invalid, it will be set as STONE.");
            mat = Material.STONE;
        }
        ItemStack is = new ItemStack(mat);
        ItemMeta meta = is.getItemMeta();
        if (config.getString(path + ".Name") != null) {
            meta.setDisplayName(StringUtils.rep(config.getString(path + ".Name")));
        }
        if (config.getStringList(path + ".Lore") != null) {
            meta.setLore(StringUtils.rep(config.getStringList(path + ".Lore")));
        }
        if (config.getInt(path + ".Amount") >= 1) {
            is.setAmount(config.getInt(path + ".Amount"));
        }
        if (config.getInt(path + ".Damage") >= 0) {
            is.setDurability((short) config.getInt(path + ".Damage"));
        }
        if (config.getBoolean(path + "HideFlags")) {
        }
        if (config.getString(path + ".PotionType") != null && is.getType().equals(Material.POTION)) {
            try {
                CompatUtils.setPotionType((PotionMeta) meta, PotionType.valueOf(config.getString(path + ".PotionType").toUpperCase()));
            } catch (IllegalArgumentException ex) {
                plugin.log("\"" + config.getString(path + ".PotionType") + "\" is not a valid PotionType");
            }
        }
        // override name and lore using player's lang
        if (player != null) {
            if (!plugin.getString(path + ".Name", CompatUtils.getLocale(player)).equals("")) {
                meta.setDisplayName(plugin.getString(path + ".Name", CompatUtils.getLocale(player)));
            }
            if (!plugin.getStringList(path + ".Lore", CompatUtils.getLocale(player)).isEmpty()) {
                meta.setLore(plugin.getStringList(path + ".Lore", CompatUtils.getLocale(player)));
            }
        }
        is.setItemMeta(meta);
        return is;
    }

    protected void setItems() {
        // NOOP
    }

    public interface GUIAction {

        void click(Player p);
    }
}
