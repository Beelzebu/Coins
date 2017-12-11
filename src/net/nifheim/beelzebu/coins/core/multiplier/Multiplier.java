/**
 * This file is part of Coins
 *
 * Copyright (C) 2017 Beelzebu
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.nifheim.beelzebu.coins.core.Core;

/**
 * Handle Coins multipliers.
 *
 * @author Beelzebu
 */
public final class Multiplier {

    private final Core core = Core.getInstance();
    private final String prefix = core.isMySQL() ? core.getConfig().getString("MySQL.Prefix") : "";
    private final String server;
    private String enabler = null;
    private Boolean enabled = false;
    private int amount = 1;
    private Long endTime = 0L;
    private int id;

    private Connection getConnection() throws SQLException {
        return core.getDatabase().getConnection();
    }

    public Multiplier(String server) {
        this.server = server;
        enabler = getEnabler(server);
        enabled = isEnabled(server);
        amount = getAmount(server);
        id = getID(server);
        endTime = checkMultiplierTime(server);
    }

    public Multiplier(String server, String enabler, boolean enabled, int amount, long endTime) {
        this.server = server;
        this.enabler = enabler;
        this.enabled = enabled;
        this.amount = amount;
        this.id = -1;
        this.endTime = endTime;
        checkMultiplierTime(server);
    }

    /**
     * Get the nick of the player who enabled this multiplier.
     *
     * @return The nick of the player.
     */
    public String getEnabler() {
        return enabler;
    }

    /**
     * Set the enabler for this multiplier instance.
     *
     * @param enabler The new enabler for this multiplier.
     */
    public void setEnabler(String enabler) {
        this.enabler = enabler;
        id = -1;
    }

    /**
     * Return <i>true</i> if the server has a multiplier enabled and
     * <i>false</i> if not.
     *
     * @return
     */
    public Boolean isEnabled() {
        return enabled;
    }

    /**
     * Set the state of this multiplier instance.
     *
     * @param enabled The new status for this multiplier.
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
        id = -1;
    }

    /**
     * Get the the amount of the multiplier enabled in this server.
     *
     * @return
     */
    public int getAmount() {
        return amount;
    }

    /**
     * Set the amout for this multiplier instance. If it is below 2, it will be
     * set to 2.
     *
     * @param amount The new amount for the multiplier.
     */
    public void setAmount(int amount) {
        if (amount < 2) {
            this.amount = 2;
        } else {
            this.amount = amount;
        }
        id = -1;
    }

    /**
     * Set the endtime for this multiplier instance.
     *
     * @param endtime The new end time.
     */
    public void setEndTime(Long endtime) {
        endTime = endtime;
        id = -1;
    }

    /**
     * Get the multiplier ID for this server.
     *
     * @return
     */
    public int getID() {
        return id;
    }

    /**
     * Get the name of the server for this multiplier.
     *
     * @return The name of the server.
     */
    public String getServer() {
        return server;
    }

    /**
     * Update this multiplier in all spigot servers.
     */
    public void sendMultiplier() {
        core.updateMultiplier(this);
    }

    /**
     * Get the real data for this multiplier.
     *
     * @return The multiplier data direct from the database.
     */
    public MultiplierData getData() {
        if (id == -1) {
            return new MultiplierData(server, enabler, enabled, amount, (int) (checkTime() / 60000), id, false) {
            };
        } else {
            return getDataByID(id);
        }
    }

    /**
     * Check for the time of this multiplier, and disable it if is expired.
     *
     * @return the remaining millis of this multiplier.
     */
    public Long checkTime() {
        return checkMultiplierTime(server);
    }

    /**
     * Create a multiplier for a player with the specified time.
     *
     * @param uuid The player to create the multiplier.
     * @param multiplier The multiplier.
     * @param minutes The time for the multiplier.
     * @param server The server to create the multiplier, if is null, we use the
     * server specified in the config.
     */
    public void createMultiplier(UUID uuid, int multiplier, int minutes, String server) {
        try (Connection c = getConnection()) {
            try {
                c.prepareStatement("INSERT INTO " + prefix + "Multipliers VALUES(NULL, '" + uuid + "', " + multiplier + ", -1, " + minutes + ", 0, " + "'" + (server != null ? server : this.server) + "'" + ", false);").executeUpdate();
            } finally {
                c.close();
            }
        } catch (SQLException ex) {
            core.log("&cSomething was wrong when creating a multiplier for " + core.getNick(uuid));
            core.debug("The error code is: " + ex.getErrorCode());
            core.debug(ex.getMessage());
        }
    }

    /**
     * Get the active multiplier countdown time formated in "days,
     * hours:minutes:seconds" Ex: 10, 03:59:30
     *
     * @return The multiplier time formated.
     */
    public String getMultiplierTimeFormated() {
        return formatTime(checkMultiplierTime(server));
    }

    /**
     * Get the multipliers of a player in this server.
     * <p>
     * If the server is set to null it shows all the multipliers for this
     * player.
     * </p>
     *
     * @param uuid The player to get the multipliers.
     * @param all If is false, only return the multipliers by the server that
     * the player is.
     * @return
     */
    public Set<Integer> getMultipliersFor(UUID uuid, boolean all) {
        return getMultipliersFor(uuid, server, all);
    }

    /**
     * Use the multiplier of a player in the server by the multiplier id.
     *
     * @param id The id of the multiplier.
     * @param type The type of the multiplier.
     * @return <i>true</i> if the multiplier was enabled and <i>false</i> if
     * not.
     */
    public boolean useMultiplier(final int id, final MultiplierType type) {
        try (Connection c = getConnection()) {
            ResultSet res = null;
            try {
                res = c.prepareStatement("SELECT * FROM " + prefix + "Multipliers WHERE id = " + id + ";").executeQuery();
                if (!isEnabled(getDataByID(id).getServer())) {
                    if (res.next()) {
                        Long minutes = res.getLong("minutes");
                        Long endtime = System.currentTimeMillis() + (minutes * 60000);
                        c.prepareStatement("UPDATE " + prefix + "Multipliers SET endtime = " + endtime + ", enabled = true WHERE id = " + id + ";").executeUpdate();
                        enabled = true;
                        amount = getAmount(res.getString("server"));
                        enabler = core.getNick(UUID.fromString(res.getString("uuid")));
                        endTime = endtime;
                        core.updateMultiplier(new Multiplier(res.getString("server")));
                        return true;
                    }
                } else {
                    res = c.prepareStatement("SELECT * FROM " + prefix + "Multipliers WHERE server = '" + server + "' ORDER BY queue DESC;").executeQuery();
                    if (res.next()) {
                        c.prepareStatement("UPDATE " + prefix + "Multipliers SET queue = " + (res.getInt("queue") + 1) + " WHERE id = " + id + ";").executeUpdate();
                        return false;
                    }
                }
            } finally {
                if (res != null) {
                    res.close();
                }
                c.close();
            }
        } catch (SQLException ex) {
            core.log("&cSomething was wrong when using a multiplier with the id: '" + id + "'");
            core.debug(ex);
        }
        return false;
    }

    private Long checkMultiplierTime(String server) {
        if (id == -1) { // this multiplier is fake
            if ((endTime - System.currentTimeMillis()) > 0) {
                return endTime - System.currentTimeMillis();
            } else {
                amount = 1;
                enabled = false;
                enabler = null;
                endTime = 0L;
                return 0L;
            }
        } else if (endTime > 0 && (endTime - System.currentTimeMillis()) > 0) { // this is the cached time of a real multiplier
            return endTime - System.currentTimeMillis();
        } else { // we don't know about any multiplier :/
            try (Connection c = getConnection()) {
                ResultSet res = null;
                try {
                    res = c.prepareStatement("SELECT * FROM " + prefix + "Multipliers WHERE server = '" + server + "' AND enabled = true;").executeQuery();
                    if (res.next()) {
                        endTime = res.getLong("endtime");
                        if ((endTime - System.currentTimeMillis()) > 0) {
                            return (endTime - System.currentTimeMillis());
                        } else {
                            c.prepareStatement("DELETE FROM " + prefix + "Multipliers WHERE server = '" + server + "' AND enabled = true;").executeUpdate();
                            amount = 1;
                            enabled = false;
                            enabler = null;
                            endTime = 0L;
                            res = c.prepareStatement("SELECT * FROM " + prefix + "Multipliers WHERE server = '" + server + "' AND enabled = false AND queue > -1 ORDER BY queue ASC;").executeQuery();
                            if (res.next()) {
                                useMultiplier(res.getInt("id"), MultiplierType.SERVER);
                            }
                        }
                    }
                } finally {
                    if (res != null) {
                        res.close();
                    }
                    c.close();
                }
            } catch (SQLException ex) {
                core.log("&cSomething was wrong when we're getting the multiplier time for " + server);
                core.debug("The error code is: " + ex.getErrorCode());
                core.debug(ex.getMessage());
            }
        }
        return 0L;
    }

    private Set<Integer> getMultipliersFor(UUID uuid, String server, boolean all) {
        Set<Integer> multipliers = new HashSet<>();
        try (Connection c = getConnection()) {
            ResultSet res = null;
            try {
                String query = "SELECT * FROM " + prefix + "Multipliers WHERE uuid = '" + uuid + "' AND enabled = false AND queue = -1";
                if (server != null && all == false) {
                    query += " AND server = '" + server + "'";
                }
                res = c.prepareStatement(query + ";").executeQuery();
                while (res.next()) {
                    multipliers.add(res.getInt("id"));
                }
            } finally {
                if (res != null) {
                    res.close();
                }
                c.close();
            }
        } catch (SQLException ex) {
            core.log("&cSomething was wrong when getting the multipliers for " + core.getNick(uuid));
            core.debug("The error code is: " + ex.getErrorCode());
            core.debug(ex.getMessage());
        }
        return multipliers;
    }

    private String getEnabler(String server) {
        try (Connection c = getConnection()) {
            ResultSet res = null;
            try {
                res = c.prepareStatement("SELECT * FROM " + prefix + "Multipliers WHERE enabled = true AND server = '" + server + "';").executeQuery();
                if (res.next()) {
                    core.log(core.getNick(UUID.fromString(res.getString("uuid"))));
                    return core.getNick(UUID.fromString(res.getString("uuid")));
                }
            } finally {
                if (res != null) {
                    res.close();
                }
                c.close();
            }
        } catch (SQLException ex) {
            core.log("&cSomething was wrong where getting the enabler for " + server);
            core.debug("The error code is: " + ex.getErrorCode());
            core.debug(ex.getMessage());
        }
        return null;
    }

    private Boolean isEnabled(String server) {
        try (Connection c = getConnection()) {
            ResultSet res = null;
            try {
                res = c.prepareStatement("SELECT * FROM " + prefix + "Multipliers WHERE enabled = true AND server = '" + server + "';").executeQuery();
                if (res.next()) {
                    return true;
                }
            } finally {
                if (res != null) {
                    res.close();
                }
                c.close();
            }
        } catch (SQLException ex) {
            core.log("&cSomething was wrong where getting if the server " + server + " has a multiplier enabled.");
            core.debug("The error code is: " + ex.getErrorCode());
            core.debug(ex);
        }
        return false;
    }

    private int getAmount(String server) {
        try (Connection c = getConnection()) {
            ResultSet res = null;
            try {
                res = c.prepareStatement("SELECT * FROM " + prefix + "Multipliers WHERE enabled = true AND server = '" + server + "';").executeQuery();
                if (res.next()) {
                    return res.getInt("multiplier");
                }
            } finally {
                if (res != null) {
                    res.close();
                }
                c.close();
            }
        } catch (SQLException ex) {
            core.log("&cSomething was wrong where getting the multiplier amount for " + server);
            core.debug("The error code is: " + ex.getErrorCode());
            core.debug(ex.getMessage());
        }
        return 1;
    }

    private int getID(String server) {
        try (Connection c = getConnection()) {
            ResultSet res = null;
            try {
                res = c.prepareStatement("SELECT * FROM " + prefix + "Multipliers WHERE server = '" + server + "' AND enabled = true;").executeQuery();
                if (res.next()) {
                    return res.getInt("id");
                }
            } finally {
                if (res != null) {
                    res.close();
                }
                c.close();
            }
        } catch (SQLException ex) {

        }
        return -1;
    }

    private static class Builder {

        private final String server;
        private final String enabler;
        private int amount = 1;
        private boolean enabled = false;
        private int minutes = 0;
        private final int id;
        private final boolean queue;

        public Builder(String server, String enabler, int id, int amount, boolean enabled, int minutes, boolean queue) {
            this.server = server;
            this.enabler = enabler;
            this.id = id;
            this.amount = amount;
            this.enabled = enabled;
            this.minutes = minutes;
            this.queue = queue;
        }

        public MultiplierData create() {
            return new MultiplierData(server, enabler, enabled, amount, minutes, id, queue) {
            };
        }
    }

    public MultiplierData getDataByID(int id) {
        try (Connection c = getConnection()) {
            ResultSet res = null;
            try {
                res = c.prepareStatement("SELECT * FROM " + prefix + "Multipliers WHERE id = " + id + ";").executeQuery();
                if (res.next()) {
                    return new Builder(res.getString("server"), core.getNick(UUID.fromString(res.getString("uuid"))), id, res.getInt("multiplier"), res.getBoolean("enabled"), res.getInt("minutes"), res.getBoolean("queue")).create();
                }
            } finally {
                if (res != null) {
                    res.close();
                }
                c.close();
            }
        } catch (SQLException ex) {
            core.log("&cSomething was wrong generating the data for the multiplier with the id: '" + id + "'");
            core.debug("The error code is: " + ex.getErrorCode());
            core.debug(ex.getMessage());
        }
        return null;
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
}
