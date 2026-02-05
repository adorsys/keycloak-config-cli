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

package io.github.doriangrelu.keycloak.config.service.state;

import io.github.doriangrelu.keycloak.config.exception.MissingRequiredExecutionContextException;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Static holder for the current {@link ExecutionContext} instance.
 *
 * <p>This class provides a globally accessible, thread-safe reference to the
 * {@link ExecutionContext} used throughout the import process. The context must
 * be explicitly initialized via {@link #initializeEmptyContext()} before any
 * access is attempted; otherwise, a {@link MissingRequiredExecutionContextException}
 * is thrown.</p>
 *
 * <p>This class cannot be instantiated.</p>
 *
 * @author Dorian GRELU
 * @see ExecutionContext
 * @see MissingRequiredExecutionContextException
 * @since 02.2026
 */
public class ExecutionContextHolder {

    private static final AtomicReference<ExecutionContext> executionContext = new AtomicReference<>();

    private ExecutionContextHolder() {
        throw new UnsupportedOperationException();
    }

    /**
     * Initializes the holder with a new, empty {@link ExecutionContext}.
     *
     * <p>This method should be called once at the beginning of the import process,
     * typically from {@link io.github.doriangrelu.keycloak.config.KeycloakConfigRunner#run(String...)}.</p>
     */
    public static void initializeEmptyContext() {
        executionContext.set(new ExecutionContext());
    }

    /**
     * Returns the current {@link ExecutionContext}.
     *
     * @return the current execution context, never {@code null}
     * @throws MissingRequiredExecutionContextException if the context has not been initialized
     */
    public static ExecutionContext context() {
        if (null == executionContext.get()) {
            throw MissingRequiredExecutionContextException.create();
        }
        return executionContext.get();
    }

}
