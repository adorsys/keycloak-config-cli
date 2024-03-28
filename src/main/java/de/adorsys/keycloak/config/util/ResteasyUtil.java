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

package de.adorsys.keycloak.config.util;

import de.adorsys.keycloak.config.util.resteasy.CookieClientFilter;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;

import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class ResteasyUtil {
    private ResteasyUtil() {
    }

    public static ResteasyClient getClient(boolean sslVerification, URL httpProxy, Duration connectTimeout, Duration readTimeout) {
        ResteasyClientBuilder clientBuilder = new ResteasyClientBuilderImpl();
        clientBuilder
                .connectionPoolSize(10)
                .connectTimeout(connectTimeout.get(ChronoUnit.NANOS), TimeUnit.NANOSECONDS)
                .readTimeout(readTimeout.get(ChronoUnit.NANOS), TimeUnit.NANOSECONDS);

        if (sslVerification) {
            clientBuilder
                    .disableTrustManager()
                    .hostnameVerification(ResteasyClientBuilder.HostnameVerificationPolicy.ANY);
        }

        if (httpProxy != null) {
            clientBuilder.defaultProxy(
                    httpProxy.getHost(),
                    httpProxy.getPort(),
                    httpProxy.getProtocol()
            );
        } else {
            clientBuilder.defaultProxy(
                    System.getProperty("http.proxyHost"),
                    Integer.parseInt(System.getProperty("http.proxyPort", "0"))
            );
        }

        clientBuilder.register(CookieClientFilter.class);

        return clientBuilder.build();
    }
}
