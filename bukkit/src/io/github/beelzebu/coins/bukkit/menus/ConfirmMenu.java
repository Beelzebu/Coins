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
import io.github.beelzebu.coins.api.MultiplierType;
import io.github.beelzebu.coins.api.utils.StringUtils;
import io.github.beelzebu.coins.bukkit.utils.CompatUtils;
import io.github.beelzebu.coins.bukkit.utils.ItemBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

/**
 * @author Beelzebu
 */
public class ConfirmMenu extends CoinsMenu {

    private final Multiplier multiplier;
    private final Player p;

    ConfirmMenu(Player player, String name, Multiplier data) {
        super(9, name);
        p = player;
        multiplier = data;
    }

    @Override
    public void setItems() {
        if (p == null) {
            return;
        }
        ItemStack accept = ItemBuilder.newBuilder(CompatUtils.getItem(CompatUtils.MaterialItem.GREEN_STAINED_GLASS)).setDisplayName(plugin.getString("Multipliers.Menu.Confirm.Accept", CompatUtils.getLocale(p))).build();
        setItem(2, accept, player -> {
            if (CoinsAPI.getMultipliers().stream().filter(multiplier -> !multiplier.getType().equals(MultiplierType.GLOBAL) && !multiplier.getType().equals(MultiplierType.PERSONAL)).collect(Collectors.toSet()).isEmpty()) {
                multiplier.enable(player.getUniqueId(), player.getName(), false);
                try {
                    player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("Multipliers.GUI.Use.Sound")), 10, 2);
                } catch (IllegalArgumentException ex) {
                    plugin.log("Seems that you're using an invalid sound, please edit the config and set the sound that corresponds for the version of your server.");
                    plugin.log("Please check https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html\n"
                            + "If need more help, please open an issue in https://github.com/Beelzebu/Coins/issues");
                }
            } else {
                multiplier.enable(player.getUniqueId(), player.getName(), true);
                try {
                    player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("Multipliers.GUI.Use.Fail.Sound")), 10, 1);
                } catch (IllegalArgumentException ex) {
                    plugin.log("Seems that you're using an invalid sound, please edit the config and set the sound that corresponds for the version of your server.");
                    plugin.log("Please check https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html\n"
                            + "If need more help, please open an issue in https://github.com/Beelzebu/Coins/issues");
                }
                player.sendMessage(plugin.getString("Multipliers.Already active", CompatUtils.getLocale(player)));
            }
            player.closeInventory();
        });
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
        CompatUtils.setPotionType(potionMeta, PotionType.FIRE_RESISTANCE);
        potionMeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        potionMeta.setDisplayName(StringUtils.rep(plugin.getString("Multipliers.Menu.Multipliers.Name", CompatUtils.getLocale(p)), multiplier));
        List<String> lore = new ArrayList<>();
        plugin.getMessages(CompatUtils.getLocale(p)).getStringList("Multipliers.Menu.Multipliers.Lore").forEach(line -> lore.add(StringUtils.rep(line, multiplier)));
        potionMeta.setLore(lore);
        potion.setItemMeta(potionMeta);
        setItem(4, potion);
        ItemStack decline = ItemBuilder.newBuilder(CompatUtils.getItem(CompatUtils.MaterialItem.RED_STAINED_GLASS)).setDisplayName(plugin.getString("Multipliers.Menu.Confirm.Decline", CompatUtils.getLocale(p))).build();
        setItem(6, decline, player -> {
            player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfig().getString("Multipliers.GUI.Use.Fail.Sound", "VILLAGER_NO")), 10, 1);
            player.closeInventory();
        });
    }
}
