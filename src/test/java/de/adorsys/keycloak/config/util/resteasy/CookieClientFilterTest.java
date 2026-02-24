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

package de.adorsys.keycloak.config.util.resteasy;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.NewCookie;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CookieClientFilterTest {

    @Test
    void responseFilter_shouldStoreCookiesInThreadLocalAndRequestFilter_shouldAttachToHeaders() {
        CookieClientFilter filter = new CookieClientFilter();

        var responseContext = mock(jakarta.ws.rs.client.ClientResponseContext.class);
        when(responseContext.getCookies()).thenReturn(new HashMap<>(
                java.util.Map.of(
                        "sticky", new NewCookie(new Cookie("sticky", "abc"))
                )
        ));

        filter.filter(mock(jakarta.ws.rs.client.ClientRequestContext.class), responseContext);

        var requestContext = mock(jakarta.ws.rs.client.ClientRequestContext.class);
        var headers = new MultivaluedHashMap<String, Object>();
        when(requestContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext);

        assertThat(headers.get("Cookie"), is(java.util.List.of("sticky=abc")));
    }

    @Test
    void responseFilter_shouldClearPreviousCookies() {
        CookieClientFilter filter = new CookieClientFilter();

        var responseContext1 = mock(jakarta.ws.rs.client.ClientResponseContext.class);
        when(responseContext1.getCookies()).thenReturn(new HashMap<>(
                java.util.Map.of(
                        "a", new NewCookie(new Cookie("a", "1"))
                )
        ));

        filter.filter(mock(jakarta.ws.rs.client.ClientRequestContext.class), responseContext1);

        var responseContext2 = mock(jakarta.ws.rs.client.ClientResponseContext.class);
        when(responseContext2.getCookies()).thenReturn(new HashMap<>(
                java.util.Map.of(
                        "b", new NewCookie(new Cookie("b", "2"))
                )
        ));

        filter.filter(mock(jakarta.ws.rs.client.ClientRequestContext.class), responseContext2);

        var requestContext = mock(jakarta.ws.rs.client.ClientRequestContext.class);
        var headers = new MultivaluedHashMap<String, Object>();
        when(requestContext.getHeaders()).thenReturn(headers);

        filter.filter(requestContext);

        assertThat(headers.get("Cookie"), is(java.util.List.of("b=2")));
    }
}
