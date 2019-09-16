package de.adorsys.keycloak.config.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopesResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScopeMappingRepository {

    private final RealmRepository realmRepository;
    private final ClientRepository clientRepository;
    private final RoleRepository roleRepository;
    private final ObjectMapper mapper;

    @Autowired
    public ScopeMappingRepository(RealmRepository realmRepository, ClientRepository clientRepository, RoleRepository roleRepository, ObjectMapper mapper) {
        this.realmRepository = realmRepository;
        this.clientRepository = clientRepository;
        this.roleRepository = roleRepository;
        this.mapper = mapper;
    }

    public void addScopeMapping(String realm, ScopeMappingRepresentation scopeMapping) {
        String clientId = scopeMapping.getClient();

        RealmRepresentation realmRepresentation = realmRepository.partialExport(realm);

        ClientScopesResource clientScopesResource = realmRepository.loadRealm(realm).clientScopes();
        List<ClientScopeRepresentation> clientScopes = clientScopesResource.findAll();
        ClientRepresentation client = clientRepository.getClient(realm, clientId);

        for (String role : scopeMapping.getRoles()) {
            RoleRepresentation realmRole = roleRepository.findRealmRole(realm, role);

            if(containsRole(clientScopes, realmRole)) {
                ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
                clientScope.setId(realmRole.getId());
                clientScope.setName(clientId);

                try {
                    System.out.println(mapper.writeValueAsString(clientScope));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                clientScopesResource.create(clientScope);
            }
        }
//
//
//
//

        ClientResource clientResource = clientRepository.getClientResource(realm, clientId);

        List<RoleRepresentation> clientRoles = clientResource.getScopeMappings().clientLevel(client.getId()).listAll();
        List<RoleRepresentation> realmRoles = clientResource.getScopeMappings().realmLevel().listAll();
        MappingsRepresentation mappings = clientResource.getScopeMappings().getAll();

        try {
            System.out.println(mapper.writeValueAsString(realmRepresentation));
            System.out.println(mapper.writeValueAsString(client));
            System.out.println(mapper.writeValueAsString(realmRoles));
            System.out.println(mapper.writeValueAsString(clientRoles));
            System.out.println(mapper.writeValueAsString(mappings));
            System.out.println(mapper.writeValueAsString(clientScopes));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private boolean containsRole(List<ClientScopeRepresentation> clientScopes, RoleRepresentation clientRole) {
        return clientScopes.stream().filter(cs -> cs.getId().equals(clientRole.getId())).count() < 1;
    }

    public void addScopeMappingRole(String realm, String clientId, List<String> roles) {
        RoleMappingResource scopeMappingsResource = clientRepository.getClientResource(realm, clientId).getScopeMappings();

        List<RoleRepresentation> realmRoles = roles.stream()
                .map(role -> roleRepository.findRealmRole(realm, role))
                .collect(Collectors.toList());

        scopeMappingsResource.realmLevel().add(realmRoles);
    }
}
