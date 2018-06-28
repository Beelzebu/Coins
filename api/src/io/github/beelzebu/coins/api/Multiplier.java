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
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.beelzebu.coins.api.plugin.CoinsPlugin;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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

    private static final CoinsPlugin PLUGIN = CoinsAPI.getPlugin();
    private MultiplierData baseData;
    @Setter(AccessLevel.PACKAGE)
    private int id;
    private String server;
    private MultiplierType type;
    private String enablerName = "SERVER";
    private UUID enablerUUID = UUID.randomUUID();
    @Setter(AccessLevel.NONE)
    private boolean enabled = false;
    @Setter(AccessLevel.PACKAGE)
    private boolean queue = false;
    @Setter(AccessLevel.PACKAGE)
    private long endTime = 0;
    private Set<MultiplierData> extradata;

    public Multiplier(String server, MultiplierData baseData) {
        this.baseData = baseData;
        this.server = server;
    }

    public static Multiplier fromJson(String multiplier, boolean callenable) {
        Preconditions.checkNotNull(multiplier, "Tried to load a null Multiplier");
        PLUGIN.debug("Loading multiplier from JSON: " + multiplier);
        try {
            JsonObject data = PLUGIN.getGson().fromJson(multiplier, JsonObject.class);
            MultiplierBuilder multi = MultiplierBuilder
                    .newBuilder(data.get("server").getAsString(), MultiplierType.valueOf(data.get("type").getAsString()), new MultiplierData(UUID.fromString(data.get("enableruuid").getAsString()), data.get("enabler").getAsString(), data.get("amount").getAsInt(), data.get("minutes").getAsInt()))
                    .setID(data.get("id").getAsInt())
                    .setEnablerName(data.get("enabler").getAsString())
                    .setEnablerUUID(UUID.fromString(data.get("enableruuid").getAsString()))
                    .setQueue(data.get("queue").getAsBoolean())
                    .setEnabled(data.get("enabled").getAsBoolean());
            if (data.get("endtime") != null) {
                multi.setEndTime(data.get("endtime").getAsLong());
            }
            return multi.build(callenable);
        } catch (JsonSyntaxException ex) {
            PLUGIN.debug(ex);
        }
        return null;
    }

    /**
     * Enable this multiplier with the default uuid and name from
     * {@link Multiplier#getEnablerUUID()} and {@link Multiplier#getEnablerName()}
     *
     * @param queue if the multiplier should be queued or inmediatly enabled.
     */
    public void enable(boolean queue) {
        enable(getEnablerUUID(), getEnablerName(), queue);
    }

    /**
     * Enable this multiplier with the specified UUID and Name.
     *
     * @param enablerUUID the UUID of the enabler.
     * @param enablerName the name of the enabler, can't be null.
     * @param queue       if the multiplier should be queued or inmediatly enabled.
     */
    public void enable(UUID enablerUUID, String enablerName, boolean queue) {
        Preconditions.checkNotNull(enablerName, "The enabler name can't be null");
        if (System.currentTimeMillis() + baseData.getMinutes() * 60000 > endTime && endTime > 1) {
            return;
        }
        this.enablerUUID = enablerUUID;
        this.enablerName = enablerName;
        enabled = true;
        endTime = System.currentTimeMillis() + baseData.getMinutes() * 60000;
        if (queue && (CoinsAPI.getMultiplier(server) == null || !CoinsAPI.getMultiplier(server).isEnabled())) {
            queue = false;
        }
        this.queue = queue;
        if (!queue) {
            if (PLUGIN.getCache().getMultiplier(server) != null && PLUGIN.getCache().getMultiplier(server).getId() != id) {
                switch (PLUGIN.getCache().getMultiplier(server).getType()) {
                    case GLOBAL:
                        PLUGIN.getCache().getMultiplier(server).addExtraData(baseData);
                        PLUGIN.getCache().getMultiplier(server).addExtraData(extradata);
                        break;
                    case SERVER:
                        PLUGIN.getCache().deleteMultiplier(PLUGIN.getCache().getMultiplier(server));
                        PLUGIN.getCache().addMultiplier(server, this);
                        break;
                    case PERSONAL:
                        extradata.add(PLUGIN.getCache().getMultiplier(server).getBaseData());
                        extradata.addAll(PLUGIN.getCache().getMultiplier(server).getExtradata());
                        PLUGIN.getCache().addMultiplier(server + " " + enablerUUID, this);
                        break;
                    default:
                        break;
                }
            }
            PLUGIN.getDatabase().enableMultiplier(this);
            PLUGIN.getBootstrap().callMultiplierEnableEvent(this);
        } else {
            PLUGIN.getCache().addQueueMultiplier(this);
        }
    }

    /**
     * Disable and then delete this multiplier from the database.
     */
    public void disable() {
        try {
            PLUGIN.getDatabase().deleteMultiplier(this);
            PLUGIN.getCache().removeQueueMultiplier(this);
            PLUGIN.getCache().deleteMultiplier(this);
            PLUGIN.getMessagingService().disableMultiplier(this);
        } catch (Exception ex) {
            PLUGIN.log("An unexpected exception has ocurred while disabling a multiplier with the id: " + id);
            PLUGIN.log("Check plugin log files for more information, please report this bug on https://github.com/Beelzebu/Coins/issues");
            PLUGIN.debug(ex);
        }
    }

    public long checkMultiplierTime() {
        long endtime = endTime - System.currentTimeMillis();
        Iterator<MultiplierData> edata = extradata.iterator();
        while (edata.hasNext()) {
            MultiplierData data = edata.next();
            if (data.getMinutes() * 60000 <= endtime) {
                edata.remove();
            }
        }
        extradata = Sets.newHashSet(edata);
        if (endtime <= 0) {
            disable();
        }
        return endtime >= 0 ? endtime : 0;
    }

    public String getMultiplierTimeFormated() {
        return formatTime(checkMultiplierTime());
    }

    public void addExtraData(MultiplierData data) {
        extradata.add(data);
    }

    public void addExtraData(Collection<MultiplierData> data) {
        extradata.addAll(data);
    }

    public int getAmount() {
        int amount = baseData.getAmount();
        if (!extradata.isEmpty()) {
            amount = extradata.stream().map(edata -> edata.getAmount()).reduce(amount, Integer::sum);
        }
        return amount;
    }

    public int getMinutes() {
        return baseData.getMinutes();
    }

    /**
     * Get the multiplier for this server.
     *
     * @return server for this multiplier.
     */
    public String getServer() {
        return server.toLowerCase();
    }

    /**
     * Get the UUID of who enabled this multiplier
     *
     * @return UUID of who enabled this multiplier, never should be null unless
     * specified by other plugin.
     */
    public UUID getEnablerUUID() {
        return enablerUUID == null ? baseData.getEnablerUUID() : enablerUUID;
    }

    /**
     * Get the username of who enabled this multiplier
     *
     * @return username of who enabled this multiplier, never should be null
     * unless specified by other plugin.
     */
    public String getEnablerName() {
        return enablerName == null ? baseData.getEnablerName() : enablerName;
    }

    public JsonObject toJson() {
        JsonObject multiplier = new JsonObject();
        multiplier.addProperty("id", getId());
        multiplier.addProperty("server", getServer());
        multiplier.addProperty("type", getType().toString());
        multiplier.addProperty("amount", getAmount());
        multiplier.addProperty("minutes", getMinutes());
        multiplier.addProperty("enabler", getEnablerName());
        multiplier.addProperty("enableruuid", getEnablerUUID().toString());
        multiplier.addProperty("enabled", isEnabled());
        multiplier.addProperty("queue", isQueue());
        multiplier.addProperty("endtime", endTime);
        return multiplier;
    }

    private String formatTime(long millis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        long hours = TimeUnit.MILLISECONDS.toHours(millis) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(millis));
        long days = TimeUnit.MILLISECONDS.toDays(millis);

        StringBuilder b = new StringBuilder();
        if (days > 0) {
            b.append(days);
            b.append(", ");
        }
        b.append(hours == 0 ? "00" : hours < 10 ? "0" + hours : String.valueOf(hours));
        b.append(":");
        b.append(minutes == 0 ? "00" : minutes < 10 ? "0" + minutes : String.valueOf(minutes));
        b.append(":");
        b.append(seconds == 0 ? "00" : seconds < 10 ? "0" + seconds : String.valueOf(seconds));
        return b.toString();
    }
}
