/*
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

import java.util.Objects;
import java.util.function.Consumer;
import lombok.Getter;

/**
 * Represents a response from the plugin, it contains three different response types and a message.
 *
 * <p>Possible response types</p>
 *
 * <ul>
 * <li>{@link CoinsResponseType#SUCCESS} - The code was executed successfully.</li>
 * <li>{@link CoinsResponseType#FAILED} - The code failed during the execution.</li>
 * <li>{@link CoinsResponseType#NOT_IMPLEMENTED} - This feature is not implemented.</li>
 * </ul>
 *
 * @author Beelzebu
 */
@Getter
public class CoinsResponse {

    private final CoinsResponseType response;
    private final String message;

    public CoinsResponse(CoinsResponseType response, String message) {
        this.response = response;
        this.message = message;
        if (response.equals(CoinsResponseType.FAILED) && Objects.equals(CoinsAPI.getPlugin().getString(message, ""), "")) {
            throw new IllegalArgumentException("CoinsResponse not providing a valid error message.");
        }
    }

    /**
     * If the response is success, invoke the specified consumer, otherwise do nothing.
     *
     * @param consumer - block to be executed if the response is successful
     */
    public void ifSuccess(Consumer<CoinsResponse> consumer) {
        if (response != null && response == CoinsResponseType.SUCCESS) {
            consumer.accept(this);
        }
    }

    /**
     * If the response is failed, invoke the specified consumer, otherwise do nothing.
     *
     * @param consumer - block to be executed if the response is failed.
     */
    public void ifFailed(Consumer<CoinsResponse> consumer) {
        if (response == null || response == CoinsResponseType.FAILED) {
            consumer.accept(this);
        }
    }

    public String getMessage() {
        String message = CoinsAPI.getPlugin().getString(this.message, "");
        if (!Objects.equals(message, "")) {
            return message;
        }
        return this.message;
    }

    public enum CoinsResponseType {
        SUCCESS,
        FAILED,
        NOT_IMPLEMENTED
    }
}
