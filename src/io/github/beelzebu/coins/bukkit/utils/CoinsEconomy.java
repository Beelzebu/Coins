/*
 * This file is part of Coins.
 *
 * Copyright © 2017 Beelzebu
 * Coins is licensed under the GNU General Public License.
 *
 * Coins is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Coins is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.github.beelzebu.coins.bukkit.utils;

import io.github.beelzebu.coins.CoinsAPI;
import io.github.beelzebu.coins.bukkit.Main;
import io.github.beelzebu.coins.common.CoinsCore;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.ServicePriority;

/**
 *
 * @author Beelzebu
 */
public class CoinsEconomy implements Economy {

    private final Main plugin;

    public CoinsEconomy(Main main) {
        plugin = main;
    }

    public void setup() {
        Bukkit.getServicesManager().register(Economy.class, this, plugin, ServicePriority.High);
    }

    public void shutdown() {
        Bukkit.getServicesManager().unregister(Economy.class, this);
    }

    @Override
    public boolean isEnabled() {
        return plugin.isEnabled();
    }

    @Override
    public String getName() {
        return plugin.getName();
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double d) {
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(d);
    }

    @Override
    public String currencyNamePlural() {
        return plugin.getConfig().getString("Vault.Name.Plural");
    }

    @Override
    public String currencyNameSingular() {
        return plugin.getConfig().getString("Vault.Name.Singular");
    }

    @Override
    public boolean hasAccount(String string) {
        return CoinsAPI.isindb(string);
    }

    @Override
    public boolean hasAccount(OfflinePlayer op) {
        return CoinsAPI.isindb(op.getUniqueId());
    }

    @Override
    public boolean hasAccount(String string, String string1) {
        return CoinsAPI.isindb(string);
    }

    @Override
    public boolean hasAccount(OfflinePlayer op, String string) {
        return CoinsAPI.isindb(op.getUniqueId());
    }

    @Override
    public double getBalance(String string) {
        return CoinsAPI.getCoins(string);
    }

    @Override
    public double getBalance(OfflinePlayer op) {
        return CoinsAPI.getCoins(op.getUniqueId());
    }

    @Override
    public double getBalance(String string, String string1) {
        return CoinsAPI.getCoins(string);
    }

    @Override
    public double getBalance(OfflinePlayer op, String string) {
        return CoinsAPI.getCoins(op.getUniqueId());
    }

    @Override
    public boolean has(String string, double d) {
        return CoinsAPI.getCoins(string) >= d;
    }

    @Override
    public boolean has(OfflinePlayer op, double d) {
        return CoinsAPI.getCoins(op.getUniqueId()) >= d;
    }

    @Override
    public boolean has(String string, String string1, double d) {
        return CoinsAPI.getCoins(string) >= d;
    }

    @Override
    public boolean has(OfflinePlayer op, String string, double d) {
        return CoinsAPI.getCoins(op.getUniqueId()) >= d;
    }

    @Override
    public EconomyResponse withdrawPlayer(String string, double d) {
        if (has(string, d)) {
            CoinsAPI.takeCoins(string, d);
            return new EconomyResponse(d, getBalance(string), ResponseType.SUCCESS, "");
        }
        return new EconomyResponse(d, getBalance(string), ResponseType.FAILURE, "No Coins");
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer op, double d) {
        if (has(op, d)) {
            CoinsAPI.takeCoins(op.getUniqueId(), d);
            return new EconomyResponse(d, getBalance(op), ResponseType.SUCCESS, "");
        }
        return new EconomyResponse(d, getBalance(op), ResponseType.FAILURE, "No Coins");
    }

    @Override
    public EconomyResponse withdrawPlayer(String string, String string1, double d) {
        if (has(string, d)) {
            CoinsAPI.takeCoins(string, d);
            return new EconomyResponse(d, getBalance(string), ResponseType.SUCCESS, "");
        }
        return new EconomyResponse(d, getBalance(string), ResponseType.FAILURE, "No Coins");
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer op, String string, double d) {
        if (has(op, d)) {
            CoinsAPI.takeCoins(op.getUniqueId(), d);
            return new EconomyResponse(d, getBalance(op), ResponseType.SUCCESS, "");
        }
        return new EconomyResponse(d, getBalance(op), ResponseType.FAILURE, "No Coins");
    }

    @Override
    public EconomyResponse depositPlayer(String string, double d) {
        CoinsAPI.addCoins(string, d, CoinsCore.getInstance().getConfig().vaultMultipliers());
        return new EconomyResponse(d, getBalance(string), ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer op, double d) {
        CoinsAPI.addCoins(op.getUniqueId(), d, CoinsCore.getInstance().getConfig().vaultMultipliers());
        return new EconomyResponse(d, getBalance(op), ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse depositPlayer(String string, String string1, double d) {
        CoinsAPI.addCoins(string, d, CoinsCore.getInstance().getConfig().vaultMultipliers());
        return new EconomyResponse(d, getBalance(string), ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer op, String string, double d) {
        CoinsAPI.addCoins(op.getUniqueId(), d, CoinsCore.getInstance().getConfig().vaultMultipliers());
        return new EconomyResponse(d, getBalance(op), ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse createBank(String string, String string1) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse createBank(String string, OfflinePlayer op) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse deleteBank(String string) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse bankBalance(String string) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse bankHas(String string, double d) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse bankWithdraw(String string, double d) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse bankDeposit(String string, double d) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse isBankOwner(String string, String string1) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse isBankOwner(String string, OfflinePlayer op) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse isBankMember(String string, String string1) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public EconomyResponse isBankMember(String string, OfflinePlayer op) {
        return new EconomyResponse(0, 0, ResponseType.NOT_IMPLEMENTED, "");
    }

    @Override
    public List<String> getBanks() {
        return Collections.emptyList();
    }

    @Override
    public boolean createPlayerAccount(String string) {
        CoinsAPI.createPlayer(string, CoinsCore.getInstance().getUUID(string));
        return !CoinsAPI.getCoinsString(string).equals("This player isn't in the database");
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer op) {
        CoinsAPI.createPlayer(op.getName(), op.getUniqueId());
        return !CoinsAPI.getCoinsString(op.getName()).equals("This player isn't in the database");
    }

    @Override
    public boolean createPlayerAccount(String string, String string1) {
        CoinsAPI.createPlayer(string, CoinsCore.getInstance().getUUID(string));
        return !CoinsAPI.getCoinsString(string).equals("This player isn't in the database");
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer op, String string) {
        CoinsAPI.createPlayer(op.getName(), op.getUniqueId());
        return !CoinsAPI.getCoinsString(op.getName()).equals("This player isn't in the database");
    }

}
