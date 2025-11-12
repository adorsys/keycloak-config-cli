/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2021 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config.util.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

import java.util.regex.Pattern;

public class SensitiveDataSanitizingFilter extends TurboFilter {

    private static final String REDACTED = "***REDACTED***";

    private static final String WIRE_LOGGER_NAME = "org.apache.http.wire";

    private static final ThreadLocal<Boolean> IS_SANITIZING = ThreadLocal.withInitial(() -> false);

    private static final Pattern FORM_PARAM_PATTERN = Pattern.compile(
            "(password|client_secret|refresh_token|code)=([^&\\s\"]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern JSON_TOKEN_PATTERN = Pattern.compile(
            "(\"(?:access_token|refresh_token|id_token)\"\\s*:\\s*\")([^\"]+)(\")",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile(
            "(Authorization:\\s*Bearer\\s+)([^\\s\\[\\]]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BASIC_AUTH_PATTERN = Pattern.compile(
            "(Authorization:\\s*Basic\\s+)([^\\s\\[\\]]+)",
            Pattern.CASE_INSENSITIVE
    );

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format,
                              Object[] params, Throwable t) {

        if (IS_SANITIZING.get()) {
            return FilterReply.NEUTRAL;
        }

        if (!WIRE_LOGGER_NAME.equals(logger.getName())) {
            return FilterReply.NEUTRAL;
        }

        if (format == null) {
            return FilterReply.NEUTRAL;
        }

        if (containsSensitiveData(format)) {
            try {
                IS_SANITIZING.set(true);

                String sanitizedMessage = sanitize(format);

                if (level == Level.TRACE) {
                    logger.trace(marker, sanitizedMessage, t);
                } else if (level == Level.DEBUG) {
                    logger.debug(marker, sanitizedMessage, t);
                } else if (level == Level.INFO) {
                    logger.info(marker, sanitizedMessage, t);
                } else if (level == Level.WARN) {
                    logger.warn(marker, sanitizedMessage, t);
                } else if (level == Level.ERROR) {
                    logger.error(marker, sanitizedMessage, t);
                }

                return FilterReply.DENY;
            } finally {
                IS_SANITIZING.set(false);
            }
        }

        return FilterReply.NEUTRAL;
    }

    private boolean containsSensitiveData(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("password=")
            || lowerMessage.contains("client_secret=")
            || lowerMessage.contains("refresh_token")
            || lowerMessage.contains("access_token")
            || lowerMessage.contains("id_token")
            || lowerMessage.contains("authorization:")
            || lowerMessage.contains("code=");
    }

    private String sanitize(String message) {
        String sanitized = message;

        sanitized = FORM_PARAM_PATTERN.matcher(sanitized).replaceAll("$1=" + REDACTED);

        sanitized = JSON_TOKEN_PATTERN.matcher(sanitized).replaceAll("$1" + REDACTED + "$3");

        sanitized = BEARER_TOKEN_PATTERN.matcher(sanitized).replaceAll("$1" + REDACTED);

        sanitized = BASIC_AUTH_PATTERN.matcher(sanitized).replaceAll("$1" + REDACTED);

        return sanitized;
    }
}
