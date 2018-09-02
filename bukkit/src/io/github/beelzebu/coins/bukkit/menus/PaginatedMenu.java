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

import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.api.Multiplier;
import io.github.beelzebu.coins.api.config.AbstractConfigFile;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import io.github.beelzebu.coins.api.utils.StringUtils;
import io.github.beelzebu.coins.bukkit.utils.CompatUtils;
import io.github.beelzebu.coins.bukkit.utils.ItemBuilder;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author Beelzebu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaginatedMenu {

    private static final CoinsPlugin PLUGIN = CoinsAPI.getPlugin();
    private static final AbstractConfigFile MULTIPLIERS_CONFIG = PLUGIN.getBootstrap().getFileAsConfig(new File(PLUGIN.getBootstrap().getDataFolder(), "multipliers.yml"));

    public static CoinsMenu createPaginatedGUI(Player player, boolean global) {
        List<Multiplier> contents = new ArrayList<>(global ? CoinsAPI.getMultipliersFor(player.getUniqueId()) : CoinsAPI.getMultipliersFor(player.getUniqueId(), PLUGIN.getConfig().getServerName()));
        return nextPage(player, contents, global, contents.size() >= 36, 0, null);
    }

    private static CoinsMenu nextPage(Player player, List<Multiplier> contents, boolean global, boolean hasNext, int start, CoinsMenu prevPage) {
        CoinsMenu menu = new MultipliersMenu(player, PLUGIN.getString("Menus.Multipliers." + (global ? "Global" : "Local") + ".Title", CompatUtils.getLocale(player)), contents, start, global);
        if (hasNext) {
            // TODO: add a option to configure this
            menu.setItem(53, menu.getItem(MULTIPLIERS_CONFIG, "Menus.Multipliers." + (global ? "Global" : "Local") + ".Next", player), p -> nextPage(p, contents, global, true, start + 36, menu).open(p));
        }
        if (prevPage != null) {
            menu.setItem(45, menu.getItem(MULTIPLIERS_CONFIG, "Menus.Multipliers." + (global ? "Global" : "Local") + ".Next", player), prevPage::open);
        }
        return menu;
    }

    private static void handleCloseSound(String path, Player p) {
        try {
            p.playSound(p.getLocation(), Sound.valueOf(MULTIPLIERS_CONFIG.getString(path + ".Sound")), 10, MULTIPLIERS_CONFIG.getInt(path + ".Pitch", 1));
        } catch (IllegalArgumentException ex) {
            PLUGIN.log("Seems that you're using an invalid sound, please edit the config and set the sound that corresponds for the version of your server.");
            PLUGIN.log("Please check https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html\nIf need more help, please open an issue in https://github.com/Beelzebu/Coins/issues");
        } catch (NullPointerException ignore) {
        }
        p.closeInventory();
    }

    private static final class MultipliersMenu extends CoinsMenu {

        private final Player player;
        private final List<Multiplier> contents;
        private final int start;
        private final String path;

        MultipliersMenu(Player player, String title, List<Multiplier> contents, int start, boolean global) {
            super(54, title);
            this.player = player;
            this.contents = contents;
            this.start = start;
            path = "Menus.Multipliers." + (global ? "Global" : "Local");
            setItems();
        }

        @Override
        protected void setItems() {
            for (int i = 36; i < 45; i++) {
                setItem(i, ItemBuilder.newBuilder(CompatUtils.getItem(CompatUtils.MaterialItem.MAGENTA_STAINED_GLASS_PANE)).setDisplayName("&f").build());
            }
            setItem(49, getItem(MULTIPLIERS_CONFIG, path + ".Close"), p -> handleCloseSound(path + ".Close", p));
            if (contents.size() <= 0) {
                setItem(22, ItemBuilder.newBuilder(Material.POTION).setDisplayName(plugin.getString("Multipliers.Menu.No Multipliers.Name", CompatUtils.getLocale(player))).setLore(StringUtils.rep(plugin.getMessages(CompatUtils.getLocale(player)).getStringList("Multipliers.Menu.No Multipliers.Lore"))).addItemFlag(ItemFlag.HIDE_POTION_EFFECTS).build());
            } else {
                for (int i = 0; i <= (contents.size() - 1 < 35 ? contents.size() - 1 : 35); i++) {
                    Multiplier multiplier = contents.get(start + i);
                    setItem(i, getMultiplier(player, multiplier), p -> new ConfirmMenu(p, plugin.getString("Multipliers.Menu.Confirm.Title", CompatUtils.getLocale(p)), multiplier).open(p));
                }
            }
        }

        private ItemStack getMultiplier(Player player, Multiplier multiplier) {
            String path = this.path + ".Multiplier";
            ItemStack is = super.getItem(MULTIPLIERS_CONFIG, path, player);
            ItemMeta meta = is.getItemMeta();
            meta.setDisplayName(StringUtils.rep(meta.getDisplayName(), multiplier));
            meta.setLore(StringUtils.rep(meta.getLore(), multiplier));
            is.setItemMeta(meta);
            return is;
        }
    }
}
