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
package io.github.beelzebu.coins.api;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import io.github.beelzebu.coins.api.utils.StringUtils;
import java.util.UUID;
import javax.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Handle Coins multipliers.
 *
 * @author Beelzebu
 */
@Getter
@Setter
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class Multiplier {

    private final CoinsPlugin plugin = CoinsAPI.getPlugin();
    @Setter(AccessLevel.PACKAGE)
    private int id;
    private String server;
    private MultiplierData data;
    private MultiplierType type;
    @Setter(AccessLevel.NONE)
    private boolean enabled = false;
    @Setter(AccessLevel.PACKAGE)
    private boolean queue = false;
    @Setter(AccessLevel.PACKAGE)
    private boolean custom = false;
    @Setter(AccessLevel.PACKAGE)
    private long endTime = 0;

    public Multiplier(String server, MultiplierData data) {
        this.data = data;
        this.server = server;
    }

    public static Multiplier fromJson(String multiplier) {
        Preconditions.checkNotNull(multiplier, "Tried to load a null Multiplier");
        CoinsAPI.getPlugin().debug("Loading multiplier from JSON: " + multiplier);
        try {
            return CoinsAPI.getPlugin().getGson().fromJson(multiplier, Multiplier.class);
        } catch (JsonSyntaxException ex) {
            CoinsAPI.getPlugin().debug(ex);
        }
        return null;
    }

    /**
     * Enable this multiplier with the default uuid and name from
     * {@link Multiplier#data#getEnablerUUID()} and {@link Multiplier#data#getEnablerName()}
     *
     * @param queue if the multiplier should be queued or immediately enabled.
     */
    public void enable(boolean queue) {
        enable(data.getEnablerUUID(), data.getEnablerName(), queue);
    }

    /**
     * Enable this multiplier with the specified UUID and Name.
     *
     * @param enablerUUID the UUID of the enabler.
     * @param enablerName the name of the enabler, can't be null.
     * @param queue       if the multiplier should be queued or immediately enabled.
     */
    public void enable(UUID enablerUUID, @Nonnull String enablerName, boolean queue) {
        if (System.currentTimeMillis() + data.getMinutes() * 60000 > endTime && endTime > 1) {
            return;
        }
        if (enablerUUID != null) {
            data.setEnablerUUID(enablerUUID);
        } else {
            plugin.debug("Trying to enable a multiplier using a null UUID. " + toJson());
        }
        data.setEnablerName(enablerName);
        enabled = true;
        endTime = System.currentTimeMillis() + data.getMinutes() * 60000;
        if (queue && (CoinsAPI.getMultipliers(server).isEmpty() || CoinsAPI.getMultipliers().stream().noneMatch(Multiplier::isEnabled)) || !getType().equals(MultiplierType.SERVER)) {
            queue = false;
        }
        this.queue = queue;
        if (!queue) {
            plugin.getCache().addMultiplier(this);
            plugin.getDatabase().enableMultiplier(this);
            plugin.getBootstrap().callMultiplierEnableEvent(this);
        } else {
            plugin.getCache().addQueueMultiplier(this);
        }
    }

    /**
     * Disable and then delete this multiplier from the database.
     */
    public void disable() {
        try {
            plugin.getDatabase().deleteMultiplier(this);
            plugin.getCache().removeQueueMultiplier(this);
            plugin.getCache().deleteMultiplier(this);
            plugin.getMessagingService().disableMultiplier(this);
        } catch (Exception ex) {
            plugin.log("An unexpected exception has occurred while disabling a multiplier with the id: " + id);
            plugin.log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins/issues");
            plugin.debug(ex);
        }
    }

    private long checkMultiplierTime() {
        if (endTime - System.currentTimeMillis() <= 0) {
            disable();
        }
        return endTime - System.currentTimeMillis() >= 0 ? endTime - System.currentTimeMillis() : 0;
    }

    public String getMultiplierTimeFormatted() {
        return StringUtils.formatTime(checkMultiplierTime());
    }

    /**
     * Get the multiplier for this server.
     *
     * @return server for this multiplier.
     */
    public String getServer() {
        return type == MultiplierType.GLOBAL ? plugin.getConfig().getServerName() : server.toLowerCase();
    }

    public JsonObject toJson() {
        return plugin.getGson().toJsonTree(this).getAsJsonObject();
    }
}
