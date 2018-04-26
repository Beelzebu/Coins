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

import java.util.ArrayList;
import java.util.List;
import io.github.beelzebu.coins.CoinsAPI;
import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.common.CoinsCore;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;

/**
 *
 * @author Beelzebu
 */
public class MultipliersGUI extends BaseGUI {

    private final CoinsCore core = CoinsCore.getInstance();
    private final Player p;

    public MultipliersGUI(Player player, String name) {
        super(54, name);
        p = player;
        setItems();
    }

    private void setItems() {
        if (p == null) {
            return;
        }
        if (CoinsAPI.getMultipliersFor(p.getUniqueId(), false).size() > 0) {
            int pos = -1;
            for (Multiplier multiplier : CoinsAPI.getMultipliersFor(p.getUniqueId(), false)) {
                pos++;
                ItemStack item = new ItemStack(Material.POTION);
                PotionMeta meta = (PotionMeta) item.getItemMeta();
                meta.setMainEffect(PotionEffectType.FIRE_RESISTANCE);
                meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
                meta.setDisplayName(core.rep(core.getString("Multipliers.Menu.Multipliers.Name", p.spigot().getLocale()), multiplier));
                List<String> lore = new ArrayList<>();
                core.getMessages(p.spigot().getLocale()).getStringList("Multipliers.Menu.Multipliers.Lore").forEach(line -> {
                    lore.add(core.rep(line, multiplier));
                });
                meta.setLore(lore);
                item.setItemMeta(meta);
                setItem(pos, item, player -> {
                    new ConfirmGUI(player, core.getString("Multipliers.Menu.Confirm.Title", player.spigot().getLocale()), multiplier).open(player);
                });
            }
        } else {
            ItemStack item = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta) item.getItemMeta();
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            meta.setDisplayName(core.getString("Multipliers.Menu.No Multipliers.Name", p.spigot().getLocale()));
            List<String> lore = new ArrayList<>();
            core.getMessages(p.spigot().getLocale()).getStringList("Multipliers.Menu.No Multipliers.Lore").forEach(line -> {
                lore.add(core.rep(line));
            });
            meta.setLore(lore);
            item.setItemMeta(meta);
            setItem(22, item);
        }
        ItemStack glass = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 2);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName("§f");
        glass.setItemMeta(meta);
        for (int i = 36; i < 45; i++) {
            setItem(i, glass);
        }
        setItem(49, getItem(core.getConfig(), "Multipliers.GUI.Close"), player -> {
            try {
                // try to play the sound for 1.9
                player.playSound(player.getLocation(), Sound.valueOf(core.getConfig().getString("Multipliers.GUI.Close.Sound")), 10, core.getConfig().getInt("Multipliers.GUI.Close.Pitch", 1));
            } catch (IllegalStateException ex) {
                // may be is 1.8
                try {
                    player.playSound(player.getLocation(), Sound.valueOf("CLICK"), 10, core.getConfig().getInt("Multipliers.GUI.Close.Pitch", 1));
                } catch (IllegalStateException ignore) {
                    // the sound just doesn't exists.
                }
                core.log("Seems that you're using an invalind sound, please edit the config and set the sound that corresponds for the version of your server.");
                core.log("If you're using 1.8 please check http://docs.codelanx.com/Bukkit/1.8/org/bukkit/Sound.html\n"
                        + "If you're using 1.9+ use https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html\n"
                        + "If need more help, please open an issue in https://github.com/Beelzebu/Coins/issues");
            }
            player.closeInventory();
        });
    }
}
