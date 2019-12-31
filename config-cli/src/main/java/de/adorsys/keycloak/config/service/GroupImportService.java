package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.GroupRepository;
import de.adorsys.keycloak.config.repository.RoleRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.RolesRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GroupImportService {
    private static final Logger logger = LoggerFactory.getLogger(GroupImportService.class);

    private final GroupRepository groupRepository;

    @Autowired
    public GroupImportService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    public void doImport(RealmImport realmImport) {
        createOrUpdateGroups(realmImport);
    }

    private void createOrUpdateGroups(RealmImport realmImport) {
        for (GroupRepresentation role : realmImport.getGroups()) {
            createOrUpdateGroup(realmImport, role);
        }
    }

    private void createOrUpdateGroup(RealmImport realmImport, GroupRepresentation group) {
        groupRepository.createOrUpdate(realmImport.getRealm(), group);
    }
}
