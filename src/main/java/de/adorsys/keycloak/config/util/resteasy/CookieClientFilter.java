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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

// By default, the RESTeasy cookie handling is very limited. There is no access to the underlying httpEngine.
// The underlying httpEngine uses an old CookieSpec by default which is going to refuse valid cookie today.
// Instead, build the httpEngine from scratch, we are using a RESTeasy filter to grab a re-attach cookie.
// Currently, this filter does not valide cookie or is able to remove cookies.
// A cookie managed is required to handle sticky sessions at cookie base
public class CookieClientFilter implements ClientRequestFilter, ClientResponseFilter {
    private final Map<String, String> cookies = new HashMap<>();

    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        clientRequestContext.getHeaders().put("Cookie", new ArrayList<>(cookies.values()));
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        responseContext.getCookies().forEach((name, cookie) -> cookies.put(name, String.format("%s=%s",
                cookie.toCookie().getName(),
                cookie.toCookie().getValue()
        )));
    }
}
