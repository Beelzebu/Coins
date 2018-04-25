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
import io.github.beelzebu.coins.common.database.Redis;
import io.github.beelzebu.coins.common.interfaces.IMessagingService;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import redis.clients.jedis.Jedis;

/**
 *
 * @author Beelzebu
 */
@NoArgsConstructor(access = AccessLevel.NONE)
public class RedisMessaging implements IMessagingService {

    private final CoinsCore core = CoinsCore.getInstance();
    private Redis redis;

    @Override
    public void start() {
        redis = core.getDatabase() instanceof Redis ? (Redis) core.getDatabase() : new Redis();
    }

    @Override
    public MessagingServiceType getType() {
        return MessagingServiceType.REDIS;
    }

    @Override
    public void publishUser(UUID uuid, double coins) {
        try (Jedis jedis = redis.getPool().getResource()) {
            jedis.publish("coins-data-update", "{\"uuid\":" + uuid + ",\"coins\":" + coins + "}");
        }
    }

    @Override
    public void publishMultiplier(Multiplier multiplier) {
        try (Jedis jedis = redis.getPool().getResource()) {
            jedis.publish("coins-multiplier", multiplier.toJson().toString());
        }
    }

    @Override
    public void enableMultiplier(Multiplier multiplier) {
        try (Jedis jedis = redis.getPool().getResource()) {
            jedis.publish("coins-event", "{\"event\":\"MultiplierEnableEvent\",\"multiplier\":" + multiplier.toJson() + "}");
        }
    }

    @Override
    public void getMultipliers() {
        throw new UnsupportedOperationException("getMultipliers is not finished yet");
    }

    @Override
    public void getExecutors() {
        throw new UnsupportedOperationException("getExecutors is not finished yet");
    }

}
