package de.adorsys.keycloak.config.service;

import de.adorsys.keycloak.config.exception.InvalidImportException;
import de.adorsys.keycloak.config.model.RealmImport;
import de.adorsys.keycloak.config.repository.RequiredActionRepository;
import de.adorsys.keycloak.config.util.CloneUtils;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Creates and updates required-actions in your realm
 */
@Service
public class RequiredActionsImportService {
    private static final Logger logger = LoggerFactory.getLogger(RequiredActionsImportService.class);

    private final RequiredActionRepository requiredActionRepository;

    public RequiredActionsImportService(
            RequiredActionRepository requiredActionRepository
    ) {
        this.requiredActionRepository = requiredActionRepository;
    }

    public void doImport(RealmImport realmImport) {
        String realm = realmImport.getRealm();

        for (RequiredActionProviderRepresentation requiredActionToImport : realmImport.getRequiredActions()) {
            throwErrorIfInvalid(requiredActionToImport);
            createOrUpdateRequireAction(realm, requiredActionToImport);
        }
    }

    /**
     * Cause of a weird keycloak endpoint behavior the alias and provider-id of an required-action should always be equal
     */
    private void throwErrorIfInvalid(RequiredActionProviderRepresentation requiredActionToImport) {
        if(!requiredActionToImport.getAlias().equals(requiredActionToImport.getProviderId())) {
            throw new InvalidImportException("Cannot import Required-Action '" + requiredActionToImport.getAlias() +"': alias and provider-id have to be equal");
        }
    }

    private void createOrUpdateRequireAction(String realm, RequiredActionProviderRepresentation requiredActionToImport) {
        String requiredActionAlias = requiredActionToImport.getAlias();
        Optional<RequiredActionProviderRepresentation> maybeRequiredAction = requiredActionRepository.tryToGetRequiredAction(realm, requiredActionAlias);

        if(maybeRequiredAction.isPresent()) {
            RequiredActionProviderRepresentation existingRequiredAction = maybeRequiredAction.get();

            updateRequiredActionIfNeeded(realm, requiredActionToImport, requiredActionAlias, existingRequiredAction);
        } else {
            logger.debug("Creating required action: {}", requiredActionAlias);
            createAndConfigureRequiredAction(realm, requiredActionToImport, requiredActionAlias);
        }
    }

    private void updateRequiredActionIfNeeded(String realm, RequiredActionProviderRepresentation requiredActionToImport, String requiredActionAlias, RequiredActionProviderRepresentation existingRequiredAction) {
        if(hasToBeUpdated(requiredActionToImport, existingRequiredAction)) {
            logger.debug("Updating required action: {}", requiredActionAlias);
            updateRequiredAction(realm, requiredActionToImport, existingRequiredAction);
        } else {
            logger.debug("No need to update required action: {}", requiredActionAlias);
        }
    }

    private boolean hasToBeUpdated(
            RequiredActionProviderRepresentation requiredActionToImport,
            RequiredActionProviderRepresentation existingRequiredAction
    ) {
        return !CloneUtils.deepEquals(requiredActionToImport, existingRequiredAction);
    }

    private void createAndConfigureRequiredAction(String realm, RequiredActionProviderRepresentation requiredActionToImport, String requiredActionAlias) {
        RequiredActionProviderSimpleRepresentation requiredActionToCreate = CloneUtils.deepClone(requiredActionToImport, RequiredActionProviderSimpleRepresentation.class);
        requiredActionRepository.createRequiredAction(realm, requiredActionToCreate);

        RequiredActionProviderRepresentation createdRequiredAction = requiredActionRepository.getRequiredAction(realm, requiredActionAlias);

        /*
         we need to update the required-action after creation because the creation only accepts following properties to be set:
         - providerId
         - name
        */
        updateRequiredAction(realm, requiredActionToImport, createdRequiredAction);
    }

    private void updateRequiredAction(
            String realm,
            RequiredActionProviderRepresentation requiredActionToImport,
            RequiredActionProviderRepresentation existingRequiredAction
    ) {
        RequiredActionProviderRepresentation requiredActionToBeConfigured = CloneUtils.deepClone(existingRequiredAction);

        requiredActionToBeConfigured.setProviderId(requiredActionToImport.getProviderId());
        requiredActionToBeConfigured.setName(requiredActionToImport.getName());
        requiredActionToBeConfigured.setAlias(requiredActionToImport.getAlias());
        requiredActionToBeConfigured.setEnabled(requiredActionToImport.isEnabled());
        requiredActionToBeConfigured.setDefaultAction(requiredActionToImport.isDefaultAction());
        requiredActionToBeConfigured.setPriority(requiredActionToImport.getPriority());
        requiredActionToBeConfigured.setConfig(requiredActionToImport.getConfig());

        requiredActionRepository.updateRequiredAction(realm, requiredActionToBeConfigured);
    }
}
