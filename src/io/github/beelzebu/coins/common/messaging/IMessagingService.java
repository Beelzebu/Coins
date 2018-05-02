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
import io.github.beelzebu.coins.common.messaging.MessagingService;
import java.util.UUID;

/**
 *
 * @author Beelzebu
 */
public interface IMessagingService {

    /**
     * Start this messaging service.
     */
    public void start();

    /**
     * Get the messaging service type in use.
     *
     * @return messaging service type defined by implementing classes.
     */
    public MessagingService getType();

    /**
     * Publish user coins over all servers using this messaging service.
     *
     * @param uuid user to publish.
     * @param coins coins to publish.
     */
    public void publishUser(UUID uuid, double coins);

    /**
     * Publish a multiplier over all servers using this messaging service.
     *
     * @param multiplier -
     */
    public void publishMultiplier(Multiplier multiplier);

    /**
     * Enable a multiplier in all servers using this messaging service.
     *
     * @param multiplier -
     */
    public void enableMultiplier(Multiplier multiplier);

    /**
     * Send a request to get all multipliers from other servers using this
     * messaging service, if this server is spigot will request it to bungeecord
     * and viceversa.
     */
    public void getMultipliers();

    /**
     * Send a request to get all executors from bungeecord or other bungeecord
     * instances if you're using more than one bungeecord server.
     */
    public void getExecutors();

    /**
     * Stop and shutdown this messaging service instance.
     */
    public void stop();
}
