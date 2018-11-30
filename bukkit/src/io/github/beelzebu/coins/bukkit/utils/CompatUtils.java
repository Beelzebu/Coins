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
package io.github.beelzebu.coins.bukkit.utils;

import io.github.beelzebu.coins.api.CoinsAPI;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

/**
 * @author Beelzebu
 */
public final class CompatUtils {

    private static final Map<MaterialItem, ItemStack> materials = new EnumMap<>(MaterialItem.class);
    private static boolean is1_8 = false;
    private static boolean is1_13 = false;

    public static void setup() {
        String ver = Bukkit.getServer().getClass().getPackage().getName().substring(Bukkit.getServer().getClass().getPackage().getName().lastIndexOf(".") + 1);
        CoinsAPI.getPlugin().log("Detected " + ver + " server version.");
        if (ver.startsWith("v1_8")) {
            CoinsAPI.getPlugin().log("Enabled compat mode for 1.8");
            is1_8 = true;
        } else if (ver.startsWith("v1_13")) {
            is1_13 = true;
        }
    }

    public static ItemStack getItem(MaterialItem materialItem) {
        return materials.computeIfAbsent(materialItem, mi -> {
            switch (materialItem) {
                case MAGENTA_STAINED_GLASS_PANE:
                    if (is1_13) {
                        return new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
                    } else {
                        return new ItemStack(Material.valueOf("STAINED_GLASS_PANE"), 1, (short) 2);
                    }
                case GREEN_STAINED_GLASS:
                    if (is1_13) {
                        return new ItemStack(Material.GREEN_STAINED_GLASS);
                    } else {
                        return new ItemStack(Material.valueOf("STAINED_GLASS"), 1, (short) 13);
                    }
                case RED_STAINED_GLASS:
                    if (is1_13) {
                        return new ItemStack(Material.RED_STAINED_GLASS);
                    } else {
                        return new ItemStack(Material.valueOf("STAINED_GLASS"), 1, (short) 14);
                    }
                default:
                    return new ItemStack(Material.AIR);
            }
        });
    }

    public static String getLocale(Player player) {
        if (is1_8) {
            return player.spigot().getLocale();
        } else { // this doesn't exists in 1.8
            return player.getLocale();
        }
    }

    public static void setPotionType(PotionMeta meta, PotionType type) {
        if (is1_13) {
            meta.setBasePotionData(new PotionData(type));
        } else {
            meta.setMainEffect(type.getEffectType()); // it was deprecated in 1.13
        }
    }

    public static Enchantment getEnchantment(@NonNull String string) {
        if (is1_13) {
            Optional<Enchantment> enchantmentOptional = Stream.of(Enchantment.values()).filter(enchantment -> enchantment.getKey().getKey().equalsIgnoreCase(string)).findFirst();
            if (enchantmentOptional.isPresent()) {
                return enchantmentOptional.get();
            }
        } else {
            if (Enchantment.getByName(string.toUpperCase()) != null) {
                return Enchantment.getByName(string.toUpperCase());
            }
        }
        return null;
    }

    public static ItemStack setDamage(ItemStack itemStack, int damage) {
        if (is1_13) {
            if (itemStack.getItemMeta() instanceof Damageable) {
                Damageable meta = (Damageable) itemStack.getItemMeta();
                meta.setDamage(damage);
                itemStack.setItemMeta((ItemMeta) meta);
            }
            return itemStack;
        } else {
            itemStack.setDurability((short) damage);
            return itemStack;
        }
    }

    public enum MaterialItem {
        MAGENTA_STAINED_GLASS_PANE, GREEN_STAINED_GLASS, RED_STAINED_GLASS
    }
}
