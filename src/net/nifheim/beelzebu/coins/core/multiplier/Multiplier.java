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
package net.nifheim.beelzebu.coins.core.multiplier;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.nifheim.beelzebu.coins.CoinsAPI;
import net.nifheim.beelzebu.coins.core.CacheManager;
import net.nifheim.beelzebu.coins.core.Core;
import org.apache.commons.lang.Validate;

/**
 * Handle Coins multipliers.
 *
 * @author Beelzebu
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public final class Multiplier {

    private final Core core = Core.getInstance();
    @Getter
    @Setter
    private MultiplierData baseData;
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private int id;
    @Getter
    @Setter
    private String server;
    @Getter
    @Setter
    private MultiplierType type;
    @Setter
    private String enablerName = null;
    @Setter
    private UUID enablerUUID = null;
    @Getter
    private boolean enabled = false;
    @Getter
    @Setter(AccessLevel.PACKAGE)
    private boolean queue = false;
    @Getter
    @Setter
    private Set<MultiplierData> extradata;
    private long endTime;

    public Multiplier(String server, MultiplierData baseData) {
        this.baseData = baseData;
        this.server = server;
    }

    /**
     * Update this multiplier in all spigot servers.
     */
    public void sendMultiplier() {
        core.updateMultiplier(this);
    }

    /**
     * Enable this multiplier with the specified UUID and Name.
     *
     * @param enablerUUID the UUID of the enabler.
     * @param enablerName the name of the enabler, can't be null.
     * @param queue if the multiplier should be queued or inmediatly enabled.
     */
    public void enable(UUID enablerUUID, String enablerName, boolean queue) {
        Validate.notNull(enablerName, "The enabler name can't be null");
        this.enablerUUID = enablerUUID;
        this.enablerName = enablerName;
        this.enabled = true;
        this.queue = queue;
        endTime = System.currentTimeMillis() + baseData.getMinutes() * 60000;
        if (!queue) {
            if (type.equals(MultiplierType.GLOBAL)) {
                extradata.add(CoinsAPI.getMultiplier().getBaseData());
                extradata.addAll(CoinsAPI.getMultiplier().getExtradata());
            }
            if (CacheManager.getMultiplier(server) != null && CacheManager.getMultiplier(server).getId() != id) {
                switch (CacheManager.getMultiplier(server).getType()) {
                    case GLOBAL:
                        CacheManager.getMultiplier(server).addExtraData(baseData);
                        CacheManager.getMultiplier(server).addExtraData(extradata);
                        break;
                    case SERVER:
                        CacheManager.removeMultiplier(server);
                        break;
                    case PERSONAL:
                        extradata.add(CacheManager.getMultiplier(server).getBaseData());
                        extradata.addAll(CacheManager.getMultiplier(server).getExtradata());
                        break;
                    default:
                        break;
                }
            }
            CacheManager.addMultiplier(server, this);
            core.getDatabase().enableMultiplier(this);
            sendMultiplier();
        } else {
            CacheManager.getQueuedMultipliers().add(this);
        }
    }

    /**
     * Disable and then delete this multiplier from the database.
     */
    public void disable() {
        enabled = false;
        CacheManager.getQueuedMultipliers().remove(this);
        CacheManager.removeMultiplier(server);
        delete();
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

    private String formatTime(final long millis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis));
        long hours = TimeUnit.MILLISECONDS.toHours(millis) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(millis));
        long days = TimeUnit.MILLISECONDS.toDays(millis);

        StringBuilder b = new StringBuilder();
        if (days > 0) {
            b.append(days);
            b.append(", ");
        }
        b.append(hours == 0 ? "00" : hours < 10 ? "0" + hours : hours);
        b.append(":");
        b.append(minutes == 0 ? "00" : minutes < 10 ? "0" + minutes : minutes);
        b.append(":");
        b.append(seconds == 0 ? "00" : seconds < 10 ? "0" + seconds : seconds);
        return b.toString();
    }

    /**
     * Delete this multiplier from the database.
     */
    private void delete() {
        queue = false;
        baseData = null;
        enablerName = null;
        enablerUUID = null;
        server = null;
        core.getDatabase().deleteMultiplier(this);
    }

    public UUID getEnablerUUID() {
        return enablerUUID == null ? baseData.getEnablerUUID() : enablerUUID;
    }

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
        multiplier.addProperty("endtime", endTime);
        multiplier.addProperty("enabler", getEnablerName());
        multiplier.addProperty("enableruuid", getEnablerUUID().toString());
        multiplier.addProperty("enabled", isEnabled());
        multiplier.addProperty("queue", isQueue());
        return multiplier;
    }

    public static Multiplier fromJson(JsonObject multiplier) {
        return fromJson(multiplier.toString());
    }

    public static Multiplier fromJson(String multiplier) {
        if (multiplier == null) {
            return null;
        }
        JsonObject mult = Core.getInstance().getGson().fromJson(multiplier, JsonObject.class);
        Multiplier multi = MultiplierBuilder.newBuilder()
                .setServer(mult.get("server").getAsString())
                .setType(MultiplierType.valueOf(mult.get("type").getAsString()))
                .setData(new MultiplierData(UUID.fromString(mult.get("enableruuid").getAsString()), mult.get("enabler").getAsString(), mult.get("amount").getAsInt(), mult.get("minutes").getAsInt()))
                .setID(mult.get("id").getAsInt())
                .setEnablerName(mult.get("enabler").getAsString())
                .setEnablerUUID(UUID.fromString(mult.get("enableruuid").getAsString()))
                .setEnabled(mult.get("enabled").getAsBoolean())
                .setQueue(mult.get("queue").getAsBoolean())
                .build();
        multi.endTime = mult.get("endtime").getAsLong();
        return multi;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
