package de.adorsys.keycloak.config.service.clientauthorization;

import org.apache.commons.lang3.StringUtils;

public final class PermissionTypeAndId {
    public final String type;
    public final String idOrPlaceholder;

    private PermissionTypeAndId(String type, String idOrPlaceholder) {
        this.type = type;
        this.idOrPlaceholder = idOrPlaceholder;
    }

    public boolean isPlaceholder() {
        return idOrPlaceholder.startsWith("$");
    }

    public String getPlaceholder() {
        return idOrPlaceholder.substring(1);
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
     * </dl>
     *
     * @return Parsed resource name or null if the name is not in expected format
     */
    public static PermissionTypeAndId fromPolicyName(String policyName) {
        String typeAndId = StringUtils.substringAfterLast(policyName, ".permission.");
        String type = StringUtils.substringBefore(typeAndId, '.');
        String id = StringUtils.substringAfter(typeAndId, '.');
        return StringUtils.isAnyBlank(type, id) ? null : new PermissionTypeAndId(type, id);
    }
}
