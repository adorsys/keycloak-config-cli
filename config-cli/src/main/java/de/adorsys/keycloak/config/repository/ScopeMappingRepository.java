package de.adorsys.keycloak.config.repository;

import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.ScopeMappingRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScopeMappingRepository {

    private final ClientRepository clientRepository;
    private final RoleRepository roleRepository;

    @Autowired
    public ScopeMappingRepository(
            ClientRepository clientRepository,
            RoleRepository roleRepository
    ) {
        this.clientRepository = clientRepository;
        this.roleRepository = roleRepository;
    }

    public void addScopeMappingRoles(String realm, String clientId, Collection<String> roles) {
        RoleMappingResource scopeMappingsResource = clientRepository.getClientResource(realm, clientId).getScopeMappings();

        List<RoleRepresentation> realmRoles = roles.stream()
                .map(role -> roleRepository.findRealmRole(realm, role))
                .collect(Collectors.toList());

        scopeMappingsResource.realmLevel().add(realmRoles);
    }

    public void removeScopeMappingRoles(String realm, String clientId, Collection<String> roles) {
        RoleMappingResource scopeMappingsResource = clientRepository.getClientResource(realm, clientId).getScopeMappings();

        List<RoleRepresentation> realmRoles = roles.stream()
                .map(role -> roleRepository.findRealmRole(realm, role))
                .collect(Collectors.toList());

        scopeMappingsResource.realmLevel().remove(realmRoles);
    }

    public void addScopeMapping(String realm, ScopeMappingRepresentation scopeMapping) {
        RoleMappingResource scopeMappingsResource = clientRepository.getClientResource(realm, scopeMapping.getClient()).getScopeMappings();

        List<RoleRepresentation> realmRoles = scopeMapping.getRoles().stream()
                .map(role -> roleRepository.findRealmRole(realm, role))
                .collect(Collectors.toList());

        scopeMappingsResource.realmLevel().add(realmRoles);
    }
}
