/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2026 adorsys GmbH & Co. KG @ https://adorsys.com
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package io.github.doriangrelu.keycloak.config.exception;

/**
 * Runtime exception thrown when the {@link io.github.doriangrelu.keycloak.config.service.state.ExecutionContext}
 * has not been initialized before being accessed.
 *
 * <p>This typically indicates that
 * {@link io.github.doriangrelu.keycloak.config.service.state.ExecutionContextHolder#initializeEmptyContext()}
 * was not called prior to retrieving the execution context.</p>
 *
 * @author Dorian GRELU
 * @since 02.2026
 */
public class MissingRequiredExecutionContextException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public MissingRequiredExecutionContextException(String message) {
        super(message);
    }

    /**
     * Factory method that creates a new {@link MissingRequiredExecutionContextException}
     * with a default error message.
     *
     * @return a new instance of {@link MissingRequiredExecutionContextException}
     */
    public static MissingRequiredExecutionContextException create() {
        return new MissingRequiredExecutionContextException("Missing required execution context, please initialize it");
    }

}
