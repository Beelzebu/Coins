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
package io.github.beelzebu.coins.common.messaging;

import io.github.beelzebu.coins.Multiplier;
import io.github.beelzebu.coins.common.CoinsCore;
import io.github.beelzebu.coins.common.interfaces.IMessagingService;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 *
 * @author Beelzebu
 */
@NoArgsConstructor(access = AccessLevel.NONE)
public abstract class BungeeMessaging implements IMessagingService {

    protected final CoinsCore core = CoinsCore.getInstance();

    @Override
    public void start() {
    }

    @Override
    public MessagingServiceType getType() {
        return MessagingServiceType.BUNGEECORD;
    }

    @Override
    public void publishMultiplier(Multiplier multiplier) {
        sendMessage("Coins", "Multiplier", Collections.singletonList(multiplier.toJson().toString()), true);
    }

    @Override
    public void enableMultiplier(Multiplier multiplier) {
        sendMessage("Coins", "Multiplier-Enable", Collections.singletonList(multiplier.toJson().toString()), false);
    }

    @Override
    public void getMultipliers() {
        sendMessage("Coins", "Multiplier", Collections.singletonList("getMultipliers"), false);
    }

    @Override
    public void stop() {
    }

    protected abstract void sendMessage(String channel, String subchannel, List<String> messages, boolean wait);
}
