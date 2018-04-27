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
package io.github.beelzebu.coins.common.messaging;

import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.common.CacheManager;
import io.github.beelzebu.coins.common.interfaces.IMessagingService;
import java.util.UUID;

/**
 *
 * @author Beelzebu
 */
public class DummyMessaging implements IMessagingService {

    @Override
    public void start() {
    }

    @Override
    public MessagingServiceType getType() {
        return MessagingServiceType.NONE;
    }

    @Override
    public void publishUser(UUID uuid, double coins) {
        CacheManager.updateCoins(uuid, coins);
    }

    @Override
    public void publishMultiplier(Multiplier multiplier) {
        CacheManager.addMultiplier(multiplier.getServer(), multiplier);
    }

    @Override
    public void enableMultiplier(Multiplier multiplier) {
        CacheManager.addMultiplier(multiplier.getServer(), multiplier);
        multiplier.enable(multiplier.getEnablerUUID(), multiplier.getEnablerName(), multiplier.isQueue());
    }

    @Override
    public void getMultipliers() {
    }

    @Override
    public void getExecutors() {
    }
}
