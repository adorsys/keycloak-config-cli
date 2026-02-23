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
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.core.spi.FilterReply;
import de.adorsys.keycloak.config.extensions.GithubActionsExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@ExtendWith(GithubActionsExtension.class)
class SensitiveDataSanitizingFilterTest {

    private SensitiveDataSanitizingFilter filter;
    private Logger wireLogger;
    private Logger otherLogger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        filter = new SensitiveDataSanitizingFilter();
        filter.start();

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        wireLogger = loggerContext.getLogger("org.apache.http.wire");
        wireLogger.setLevel(Level.DEBUG);

        otherLogger = loggerContext.getLogger("some.other.logger");
        otherLogger.setLevel(Level.DEBUG);

        // Add list appender to capture log events
        listAppender = new ListAppender<>();
        listAppender.start();
        wireLogger.addAppender(listAppender);
    }

    @Test
    void shouldSanitizePasswordInFormParameters() {
        String message = "grant_type=password&username=admin&password=secret123";

        FilterReply reply = filter.decide(null, wireLogger, Level.DEBUG, message, null, null);

        assertThat(reply, is(FilterReply.DENY));
        assertThat(listAppender.list.size(), is(1));

        String sanitizedMessage = listAppender.list.get(0).getFormattedMessage();
        assertThat(sanitizedMessage, containsString("password=***REDACTED***"));
        assertThat(sanitizedMessage, not(containsString("secret123")));
        assertThat(sanitizedMessage, containsString("username=admin"));
    }

    @Test
    void shouldSanitizeClientSecretInFormParameters() {
        String message = "grant_type=client_credentials&client_secret=my-secret-key";

        FilterReply reply = filter.decide(null, wireLogger, Level.DEBUG, message, null, null);

        assertThat(reply, is(FilterReply.DENY));
        assertThat(listAppender.list.size(), is(1));

        String sanitizedMessage = listAppender.list.get(0).getFormattedMessage();
        assertThat(sanitizedMessage, containsString("client_secret=***REDACTED***"));
        assertThat(sanitizedMessage, not(containsString("my-secret-key")));
    }

    @Test
    void shouldSanitizeRefreshTokenInFormParameters() {
        String message = "refresh_token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9&grant_type=refresh_token";

        FilterReply reply = filter.decide(null, wireLogger, Level.DEBUG, message, null, null);

        assertThat(reply, is(FilterReply.DENY));
        assertThat(listAppender.list.size(), is(1));

        String sanitizedMessage = listAppender.list.get(0).getFormattedMessage();
        assertThat(sanitizedMessage, containsString("refresh_token=***REDACTED***"));
        assertThat(sanitizedMessage, not(containsString("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")));
    }

    @Test
    void shouldSanitizeAccessTokenInJsonResponse() {
        String message = "{\"access_token\":\"eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9\",\"expires_in\":300}";

        FilterReply reply = filter.decide(null, wireLogger, Level.DEBUG, message, null, null);

        assertThat(reply, is(FilterReply.DENY));
        assertThat(listAppender.list.size(), is(1));

        String sanitizedMessage = listAppender.list.get(0).getFormattedMessage();
        assertThat(sanitizedMessage, containsString("\"access_token\":\"***REDACTED***\""));
        assertThat(sanitizedMessage, not(containsString("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9")));
    }

    @Test
    void shouldSanitizeRefreshTokenInJsonResponse() {
        String message = "{\"refresh_token\":\"eyJhbGciOiJIUzI1NiJ9\",\"token_type\":\"Bearer\"}";

        FilterReply reply = filter.decide(null, wireLogger, Level.DEBUG, message, null, null);

        assertThat(reply, is(FilterReply.DENY));
        assertThat(listAppender.list.size(), is(1));

        String sanitizedMessage = listAppender.list.get(0).getFormattedMessage();
        assertThat(sanitizedMessage, containsString("\"refresh_token\":\"***REDACTED***\""));
        assertThat(sanitizedMessage, not(containsString("eyJhbGciOiJIUzI1NiJ9")));
    }

    @Test
    void shouldSanitizeBearerTokenInAuthorizationHeader() {
        String message = "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.payload.signature";

        FilterReply reply = filter.decide(null, wireLogger, Level.DEBUG, message, null, null);

        assertThat(reply, is(FilterReply.DENY));
        assertThat(listAppender.list.size(), is(1));

        String sanitizedMessage = listAppender.list.get(0).getFormattedMessage();
        assertThat(sanitizedMessage, containsString("Authorization: Bearer ***REDACTED***"));
        assertThat(sanitizedMessage, not(containsString("payload.signature")));
    }

    @Test
    void shouldSanitizeBasicAuthInAuthorizationHeader() {
        String message = "Authorization: Basic YWRtaW46cGFzc3dvcmQ=";

        FilterReply reply = filter.decide(null, wireLogger, Level.DEBUG, message, null, null);

        assertThat(reply, is(FilterReply.DENY));
        assertThat(listAppender.list.size(), is(1));

        String sanitizedMessage = listAppender.list.get(0).getFormattedMessage();
        assertThat(sanitizedMessage, containsString("Authorization: Basic ***REDACTED***"));
        assertThat(sanitizedMessage, not(containsString("YWRtaW46cGFzc3dvcmQ=")));
    }

    @Test
    void shouldSanitizeAuthorizationCodeInFormParameters() {
        String message = "grant_type=authorization_code&code=abc123xyz789&redirect_uri=https://example.com";

        FilterReply reply = filter.decide(null, wireLogger, Level.DEBUG, message, null, null);

        assertThat(reply, is(FilterReply.DENY));
        assertThat(listAppender.list.size(), is(1));

        String sanitizedMessage = listAppender.list.get(0).getFormattedMessage();
        assertThat(sanitizedMessage, containsString("code=***REDACTED***"));
        assertThat(sanitizedMessage, not(containsString("abc123xyz789")));
    }

    @Test
    void shouldAllowNonSensitiveLogsThrough() {
        String message = "GET /admin/realms/master HTTP/1.1";

        FilterReply reply = filter.decide(null, wireLogger, Level.DEBUG, message, null, null);

        assertThat(reply, is(FilterReply.NEUTRAL));
        assertThat(listAppender.list.size(), is(0));
    }

    @Test
    void shouldNotFilterLogsFromOtherLoggers() {
        String message = "grant_type=password&username=admin&password=secret123";

        FilterReply reply = filter.decide(null, otherLogger, Level.DEBUG, message, null, null);

        assertThat(reply, is(FilterReply.NEUTRAL));
    }

    @Test
    void shouldHandleNullMessage() {
        FilterReply reply = filter.decide(null, wireLogger, Level.DEBUG, null, null, null);

        assertThat(reply, is(FilterReply.NEUTRAL));
    }

    @Test
    void shouldSanitizeMultipleSensitiveFieldsInSameMessage() {
        String message = "grant_type=password&username=admin&password=secret123&client_secret=my-client-secret";

        FilterReply reply = filter.decide(null, wireLogger, Level.DEBUG, message, null, null);

        assertThat(reply, is(FilterReply.DENY));
        assertThat(listAppender.list.size(), is(1));

        String sanitizedMessage = listAppender.list.get(0).getFormattedMessage();
        assertThat(sanitizedMessage, containsString("password=***REDACTED***"));
        assertThat(sanitizedMessage, containsString("client_secret=***REDACTED***"));
        assertThat(sanitizedMessage, not(containsString("secret123")));
        assertThat(sanitizedMessage, not(containsString("my-client-secret")));
    }

    @Test
    void shouldSanitizePasswordCaseInsensitive() {
        String message = "PASSWORD=Secret123&Username=admin";

        FilterReply reply = filter.decide(null, wireLogger, Level.DEBUG, message, null, null);

        assertThat(reply, is(FilterReply.DENY));
        assertThat(listAppender.list.size(), is(1));

        String sanitizedMessage = listAppender.list.get(0).getFormattedMessage();
        assertThat(sanitizedMessage, not(containsString("Secret123")));
    }

    @Test
    void shouldPreserveNonSensitivePartsOfMessage() {
        String message = "POST /token HTTP/1.1 - grant_type=password&username=testuser&password=secret";

        FilterReply reply = filter.decide(null, wireLogger, Level.DEBUG, message, null, null);

        assertThat(reply, is(FilterReply.DENY));
        assertThat(listAppender.list.size(), is(1));

        String sanitizedMessage = listAppender.list.get(0).getFormattedMessage();
        assertThat(sanitizedMessage, containsString("POST /token HTTP/1.1"));
        assertThat(sanitizedMessage, containsString("username=testuser"));
        assertThat(sanitizedMessage, containsString("password=***REDACTED***"));
        assertThat(sanitizedMessage, not(containsString("secret")));
    }

    @Test
    void shouldHandleJsonWithMultipleTokens() {
        String message = "{\"access_token\":\"token1\",\"refresh_token\":\"token2\",\"id_token\":\"token3\"}";

        FilterReply reply = filter.decide(null, wireLogger, Level.DEBUG, message, null, null);

        assertThat(reply, is(FilterReply.DENY));
        assertThat(listAppender.list.size(), is(1));

        String sanitizedMessage = listAppender.list.get(0).getFormattedMessage();
        assertThat(sanitizedMessage, containsString("\"access_token\":\"***REDACTED***\""));
        assertThat(sanitizedMessage, containsString("\"refresh_token\":\"***REDACTED***\""));
        assertThat(sanitizedMessage, containsString("\"id_token\":\"***REDACTED***\""));
        assertThat(sanitizedMessage, not(containsString("token1")));
        assertThat(sanitizedMessage, not(containsString("token2")));
        assertThat(sanitizedMessage, not(containsString("token3")));
    }
}
