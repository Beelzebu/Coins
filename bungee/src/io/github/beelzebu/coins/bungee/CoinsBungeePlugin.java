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
package io.github.beelzebu.coins.bungee;

import io.github.beelzebu.coins.api.CoinsAPI;
import io.github.beelzebu.coins.common.plugin.CommonCoinsPlugin;
import net.md_5.bungee.api.ProxyServer;

/**
 * @author Beelzebu
 */
public class CoinsBungeePlugin extends CommonCoinsPlugin {

    public CoinsBungeePlugin(CoinsBungeeMain bootstrap) {
        super(bootstrap);
    }

    @Override
    public void enable() {
        super.enable();
        CoinsAPI.setPlugin(this);
    }

    @Override
    public void disable() {
        super.disable();
        ProxyServer.getInstance().getScheduler().cancel((CoinsBungeeMain) getBootstrap());
    }
}
