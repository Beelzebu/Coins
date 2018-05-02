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
package io.github.beelzebu.coins.bukkit.menus;

import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.utils.ItemBuilder;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author Beelzebu
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaginatedMenu {

    private static final CoinsCore CORE = CoinsCore.getInstance();

    public static CoinsMenu createPaginatedGUI(Player player, List<Multiplier> contents) {
        return nextPage(player, contents, contents.size() > 53, 0);
    }

    private static CoinsMenu nextPage(Player player, List<Multiplier> contents, boolean hasnext, int start) {
        CoinsMenu menu = new CoinsMenu(54, CORE.getString("Multipliers.Menu.Title", player.spigot().getLocale())) {
            @Override
            protected void setItems() {
                for (int i = 36; i < 45; i++) {
                    setItem(i, ItemBuilder.newBuilder(Material.STAINED_GLASS_PANE).setData(2).setDisplayName("&f").build());
                }
                setItem(49, getItem(core.getConfig(), "Multipliers.GUI.Close"), p -> handleSound(p));
                if (contents.size() > 0) {
                    setItem(22, ItemBuilder.newBuilder(Material.POTION).setDisplayName(core.getString("Multipliers.Menu.No Multipliers.Name", player.spigot().getLocale())).setLore(core.rep(core.getMessages(player.spigot().getLocale()).getStringList("Multipliers.Menu.No Multipliers.Lore"))).addItemFlag(ItemFlag.HIDE_POTION_EFFECTS).build());
                } else {
                    for (int i = 0; i < 36; i++) {
                        Multiplier multiplier = contents.get(start + i);
                        setItem(i, getItemFor(player, multiplier), p -> new ConfirmMenu(p, core.getString("Multipliers.Menu.Confirm.Title", p.spigot().getLocale()), multiplier).open(p));
                    }
                }
            }
        };
        if (hasnext) {
            // TODO: add a option to configure this
            menu.setItem(53, ItemBuilder.newBuilder(Material.ARROW).setDisplayName("next").build(), p -> nextPage(p, contents, hasnext, start + 36).open(p));
        }
        return menu;
    }

    private static ItemStack getItemFor(Player player, Multiplier multiplier) {
        return ItemBuilder.newBuilder(Material.POTION).setDisplayName(CORE.rep(CORE.getString("Multipliers.Menu.Multipliers.Name", player.spigot().getLocale()), multiplier)).setLore(CORE.rep(CORE.getMessages(player.spigot().getLocale()).getStringList("Multipliers.Menu.Multipliers.Lore"))).addItemFlag(ItemFlag.HIDE_POTION_EFFECTS).build();
    }

    private static void handleSound(Player p) {
        try { // try to play the sound for 1.9
            p.playSound(p.getLocation(), Sound.valueOf(CORE.getConfig().getString("Multipliers.GUI.Close.Sound")), 10, CORE.getConfig().getInt("Multipliers.GUI.Close.Pitch", 1));
        } catch (IllegalArgumentException ex) { // may be is 1.8
            try {
                p.playSound(p.getLocation(), Sound.valueOf("CLICK"), 10, CORE.getConfig().getInt("Multipliers.GUI.Close.Pitch", 1));
            } catch (IllegalArgumentException ignore) { // the sound just doesn't exists.
            }
            CORE.log("Seems that you're using an invalind sound, please edit the config and set the sound that corresponds for the version of your server.");
            CORE.log("If you're using 1.8 please check http://docs.codelanx.com/Bukkit/1.8/org/bukkit/Sound.html\n"
                    + "If you're using 1.9+ use https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html\n"
                    + "If need more help, please open an issue in https://github.com/Beelzebu/Coins/issues");
        }
        p.closeInventory();
    }
}
