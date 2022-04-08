/*-
 * ---license-start
 * keycloak-config-cli
 * ---
 * Copyright (C) 2017 - 2022 adorsys GmbH & Co. KG @ https://adorsys.com
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

package de.adorsys.keycloak.config.service.clientauthorization;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.StringJoiner;

public final class PermissionTypeAndId {
    public final String type;
    public final String idOrPlaceholder;

    PermissionTypeAndId(String type, String idOrPlaceholder) {
        this.type = type;
        this.idOrPlaceholder = idOrPlaceholder;
    }

    /**
     * Parses resource name.
     *
     * <p>
     * For example:
     * <dl>
     *   <dt>idp.resource.1dcbfbe7-1cee-4d42-8c39-d8ed74b4cf22</dt>
     *   <dd>returns (idp, 1dcbfbe7-1cee-4d42-8c39-d8ed74b4cf22)</dd>
     *   <dt>client.resource.$my-client-id</dt>
     *   <dd>returns (client, $my-client-id)</dd>
     * </dl>
     *
     * @return Parsed resource name or null if the name is not in expected format
     */
    public static PermissionTypeAndId fromResourceName(String resourceName) {
        String type = StringUtils.substringBefore(resourceName, ".resource.");
        String id = StringUtils.substringAfter(resourceName, ".resource.");
        return StringUtils.isAnyBlank(type, id) ? null : new PermissionTypeAndId(type, id);
    }

    /**
     * Parses policy name.
     *
     * <p>
     * For example:
     * <dl>
     *   <dt>token-exchange.permission.idp.1dcbfbe7-1cee-4d42-8c39-d8ed74b4cf22</dt>
     *   <dd>returns (idp, 1dcbfbe7-1cee-4d42-8c39-d8ed74b4cf22)</dd>
     *   <dt>manage.permission.client.$my-client-id</dt>
     *   <dd>returns (client, $my-client-id)</dd>
     *   <dt>map-roles.permission.client.bd536a1f-7666-4b7b-bc6a-5875b8ef1a91</dt>
     *   <dd>returns (client, bd536a1f-7666-4b7b-bc6a-5875b8ef1a91)</dd>
     *   <dt>map-role.permission.$test role</dt>
     *   <dd>returns (role, $test role) - roles are strange</dd>
     * </dl>
     *
     * @return Parsed resource name or null if the name is not in expected format
     */
    public static PermissionTypeAndId fromPolicyName(String policyName) {
        String typeAndId = StringUtils.substringAfter(policyName, ".permission.");

        // policies for roles have a different format, they skip the type segment and instead have 'map-role' in the scope
        if (StringUtils.startsWith(policyName, "map-role") && canBeIdOrPlaceholder(typeAndId)) {
            return new PermissionTypeAndId("role", typeAndId);
        }

        String type = StringUtils.substringBefore(typeAndId, '.');
        String id = StringUtils.substringAfter(typeAndId, '.');
        return StringUtils.isAnyBlank(type, id) ? null : new PermissionTypeAndId(type, id);
    }

    private static boolean canBeIdOrPlaceholder(String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }
        if (str.startsWith("$")) {
            return true;
        }
        return !str.contains(".");
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isPlaceholder() {
        return idOrPlaceholder.startsWith("$");
    }

    public String getPlaceholder() {
        return idOrPlaceholder.substring(1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PermissionTypeAndId typeAndId = (PermissionTypeAndId) o;
        return Objects.equals(type, typeAndId.type) && Objects.equals(idOrPlaceholder, typeAndId.idOrPlaceholder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, idOrPlaceholder);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", PermissionTypeAndId.class.getSimpleName() + "[", "]")
                .add("type='" + type + "'")
                .add("idOrPlaceholder='" + idOrPlaceholder + "'")
                .toString();
    }
}
