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

import io.github.beelzebu.coins.api.utils.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author Beelzebu
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ItemBuilder {

    private final Material material;
    private String displayName;
    private List<String> lore;
    private Set<ItemFlag> flags;

    public static ItemBuilder newBuilder(Material material) {
        return new ItemBuilder(material);
    }

    public static ItemBuilder newBuilder(ItemStack itemStack) {
        return new ItemBuilder(itemStack.getType()).setDisplayName(itemStack.getItemMeta().getDisplayName()).setLore(itemStack.getItemMeta().getLore()).addItemFlag(itemStack.getItemMeta().getItemFlags());
    }

    public ItemBuilder setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        this.lore = lore;
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        this.lore = Arrays.asList(lore);
        return this;
    }

    public ItemBuilder setLore(String lore) {
        this.lore = Collections.singletonList(lore);
        return this;
    }

    public ItemBuilder addLore(String line) {
        if (lore == null) {
            lore = new ArrayList<>();
        }
        lore.add(line);
        return this;
    }

    public ItemBuilder addLore(String line, int index) {
        if (lore == null) {
            lore = new ArrayList<>();
        }
        lore.add(index, line);
        return this;
    }


    public ItemBuilder addItemFlag(ItemFlag flag) {
        if (flags == null) {
            flags = new HashSet<>();
        }
        flags.add(flag);
        return this;
    }

    public ItemBuilder addItemFlag(Collection<? extends ItemFlag> flags) {
        if (this.flags == null) {
            this.flags = new HashSet<>();
        }
        this.flags.addAll(flags);
        return this;
    }

    public ItemStack build() {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (displayName != null) {
            meta.setDisplayName(StringUtils.rep(displayName));
        }
        if (lore != null) {
            meta.setLore(StringUtils.rep(lore));
        }
        if (flags != null) {
            meta.addItemFlags(flags.toArray(new ItemFlag[]{}));
        }
        item.setItemMeta(meta);
        return item;
    }
}
