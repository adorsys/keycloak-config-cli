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

package de.adorsys.keycloak.config.extensions;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldif.LDIFReader;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

// https://gist.github.com/peterkeller/6d9993b440c4f0bf0cffc71b595df1bb
public class LdapExtension implements BeforeAllCallback, AfterAllCallback {

    private static final Logger LOG = LoggerFactory.getLogger(LdapExtension.class);
    public final String ldif;
    public final String baseDN;
    public final String bindDN;
    public final String password;
    private InMemoryDirectoryServer server;

    public LdapExtension(String baseDN, String ldif, String bindDN, String password) {
        this.ldif = ldif;
        this.baseDN = baseDN;
        this.bindDN = bindDN;
        this.password = password;
    }

    @Override
    @SuppressWarnings("null")
    public void beforeAll(final ExtensionContext context) throws Exception {
        try (final InputStream inputStream = new ClassPathResource(ldif).getInputStream()) {
            LOG.info("LDAP server starting...");
            final InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(baseDN);
            config.setSchema(null); // must be set or initialization fails with LDAPException
            config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("LDAP", 0));
            if (bindDN != null) {
                config.addAdditionalBindCredentials(bindDN, password);
            }

            final LDIFReader reader = new LDIFReader(inputStream);
            server = new InMemoryDirectoryServer(config);
            server.add("dn: dc=example,dc=org", "objectClass: domain", "objectClass: top", "dc: example");
            server.importFromLDIF(false, reader);

            server.startListening();
            LOG.info("LDAP server started. Listen on port " + server.getListenPort());
            if (System.getProperty("JUNIT_LDAP_HOST") == null) {
                //Default: Docker internal host
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("linux")) {
                    System.setProperty("JUNIT_LDAP_HOST", "172.17.0.1");
                } else {
                    System.setProperty("JUNIT_LDAP_HOST", "host.docker.internal");
                }
            }
            System.setProperty("JUNIT_LDAP_PORT", String.valueOf(server.getListenPort()));
            LOG.info("Using LDAP properties {}:{}", System.getProperty("JUNIT_LDAP_HOST"), System.getProperty("JUNIT_LDAP_PORT"));
        }
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        server.shutDown(true);
    }
}
