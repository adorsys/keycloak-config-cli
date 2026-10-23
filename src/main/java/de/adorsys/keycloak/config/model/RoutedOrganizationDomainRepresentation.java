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

package de.adorsys.keycloak.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;

import java.util.Collection;
import java.util.Set;

/**
 * Organization domain including the identity provider routing introduced in Keycloak 26.8
 * ({@code identityProviderAlias}, {@code autoRedirect}).
 * <p>
 * The {@link OrganizationDomainRepresentation} of keycloak-admin-client 26.0.x does not know these properties, so
 * reading and writing an organization through it drops the routing. Both properties are only serialized when set,
 * which keeps requests to Keycloak versions without domain routing unchanged.
 */
public class RoutedOrganizationDomainRepresentation extends OrganizationDomainRepresentation {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String identityProviderAlias;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean autoRedirect;

    public RoutedOrganizationDomainRepresentation() {
    }

    public RoutedOrganizationDomainRepresentation(String name) {
        super(name);
    }

    public String getIdentityProviderAlias() {
        return identityProviderAlias;
    }

    public void setIdentityProviderAlias(String identityProviderAlias) {
        this.identityProviderAlias = identityProviderAlias;
    }

    public Boolean getAutoRedirect() {
        return autoRedirect;
    }

    public void setAutoRedirect(Boolean autoRedirect) {
        this.autoRedirect = autoRedirect;
    }

    public boolean hasRouting() {
        return identityProviderAlias != null || Boolean.TRUE.equals(autoRedirect);
    }

    /**
     * @return a copy of this domain without identity provider routing
     */
    public RoutedOrganizationDomainRepresentation withoutRouting() {
        RoutedOrganizationDomainRepresentation copy = new RoutedOrganizationDomainRepresentation(getName());
        copy.setVerified(isVerified());
        return copy;
    }

    /**
     * Like the parent, a domain is identified by its name only (organizations hold their domains in a set), so
     * routed and plain domains with the same name are equal. The routing properties are deliberately not compared.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrganizationDomainRepresentation other)) return false;
        return getName() != null && getName().equals(other.getName());
    }

    @Override
    public int hashCode() {
        // same as the parent: hash of the name, identity for unnamed domains
        return getName() == null ? System.identityHashCode(this) : getName().hashCode();
    }

    /**
     * @return the domain itself if it already carries routing information, otherwise a copy without routing
     */
    public static RoutedOrganizationDomainRepresentation of(OrganizationDomainRepresentation domain) {
        if (domain instanceof RoutedOrganizationDomainRepresentation routed) {
            return routed;
        }
        RoutedOrganizationDomainRepresentation copy = new RoutedOrganizationDomainRepresentation(domain.getName());
        copy.setVerified(domain.isVerified());
        return copy;
    }

    /**
     * Replaces the domains of the organization; {@link OrganizationRepresentation} has no setter for them.
     */
    public static void replaceDomains(OrganizationRepresentation organization,
                                      Collection<? extends OrganizationDomainRepresentation> domains) {
        Set<OrganizationDomainRepresentation> currentDomains = organization.getDomains();
        if (currentDomains != null) {
            currentDomains.clear();
        }
        domains.forEach(organization::addDomain);
    }
}
