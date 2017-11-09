package de.adorsys.keycloak.config.authenticator;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationConfigSerivce {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationConfigSerivce.class);
    private final Keycloak keycloak;

    @Autowired
    public AuthenticationConfigSerivce(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    /**
     * Copy and modify an existing flow.
     * 
     * @param realmId
     * @param authConfiguration
     * @throws IOException 
     */
    public void createAuthenticationConfig(String realmId, AuthenticationConfig authConfiguration) throws IOException {
    	if(authConfiguration==null) return;
    	        
        final List<FlowCopyConfig> flowInputs = authConfiguration.getFlows();
        
    	// If the flow was created before, ignore it.
        AuthenticationManagementResource realmFlows = getRealm(realmId).flows();
    	List<AuthenticationFlowRepresentation> flows = realmFlows.getFlows();    	
    	for (FlowCopyConfig flowCopyConfig : flowInputs) {
        	for (AuthenticationFlowRepresentation f : flows) {
        		if(flowCopyConfig.getAlias()!=null && flowCopyConfig.getAlias().equals(f.getAlias())){
                	// Log presence and continue
                	LOG.warn("Flow with name: " + flowCopyConfig.getAlias() + " already created." );
                	continue;        			
        		}
    		}
        	
            // Load the model flow. Must exist.
            Optional<AuthenticationFlowRepresentation> modelFlow = 
            		realmFlows.getFlows().stream().filter(f -> flowCopyConfig.getModelAlias().equals(f.getAlias())).findAny();
            if(!modelFlow.isPresent()){
            	// Log absence and continue.
            	LOG.warn("Modelflow with name : " +flowCopyConfig.getAlias() + " not found. Flow with name: " + flowCopyConfig.getAlias() + " can not created." );
            	continue;
            }
            
            LOG.info("Processing copy of: " + flowCopyConfig.getModelAlias() + " to: " + flowCopyConfig.getAlias());
        	AuthenticationFlowRepresentation modelFlowRep = modelFlow.get();
        	Map<String, String> newNameMap = new HashMap<String, String>();
        	newNameMap.put("newName", flowCopyConfig.getAlias());
        	Response copyResp = realmFlows.copy(modelFlowRep.getAlias(), newNameMap);
        	if(copyResp.getStatus()!=201){
            	LOG.warn("Can not copy flow. : " + copyResp.getStatus());
        		continue;
        	}
        	updateExecutions(realmFlows, flowCopyConfig.getAlias(), flowCopyConfig.getExecutions());
        }
        setFlowBindings(authConfiguration, getRealm(realmId));
    }
    
    private void updateExecutions(AuthenticationManagementResource realmFlows, String flowAlias, List<ExcecutionConfig> executionConfigs) throws IOException{
		LOG.info("Updating executions for: " + flowAlias + " with: " + executionConfigs.size() + " executions");
    	if(flowAlias==null || executionConfigs==null || executionConfigs.isEmpty()) return;
    	for (ExcecutionConfig excecutionConfig : executionConfigs) {
    		if(excecutionConfig.getAlias()!=null){// handle subflow
    			LOG.info("Processing subflow: " + excecutionConfig.getAlias());
    			updateExecutions(realmFlows, excecutionConfig.getAlias(), excecutionConfig.getExecutions());
    		} else {// update poviders
    			LOG.info("Processing executions for: - Model: " + excecutionConfig.getModelProvider() + " NewProvider: " + excecutionConfig.getProvider());
    			if(excecutionConfig.getModelProvider()==null) continue;
    			List<AuthenticationExecutionInfoRepresentation> executions = realmFlows.getExecutions(flowAlias);
    			LOG.info("Execution count: " + executions.size());
    			for (AuthenticationExecutionInfoRepresentation aer : executions) {
        			LOG.info("Id: " + aer.getId() + " - Display: " + aer.getDisplayName() + " - Provider: " + aer.getProviderId());
    				if(!excecutionConfig.getModelProvider().equals(aer.getProviderId())) continue;
        			LOG.info("Id: " + aer.getId() + " - Setting Provider: " + excecutionConfig.getProvider() + " for: " + aer.getProviderId());
        			
        			Map<String, String> data = new HashMap<>();
        			data.put("provider", excecutionConfig.getProvider());
        			realmFlows.removeExecution(aer.getId());
                    realmFlows.addExecution(flowAlias, data);
                    AuthenticationExecutionInfoRepresentation createdExecution = realmFlows.getExecutions(flowAlias).stream().filter(e -> excecutionConfig.getProvider().equals(e.getProviderId())).findAny().get();
                    createdExecution.setIndex(aer.getIndex());
                    createdExecution.setRequirement(aer.getRequirement());
                    createdExecution.setLevel(aer.getLevel());
                    realmFlows.updateExecutions(flowAlias, createdExecution);
        			
        			Optional<AuthenticationExecutionInfoRepresentation> updateExecutionHolder = realmFlows.getExecutions(flowAlias).stream().filter(e -> createdExecution.getProviderId().equals(e.getProviderId())).findAny();
                    if(updateExecutionHolder.isPresent()) {
                    	AuthenticationExecutionInfoRepresentation updateExecution = updateExecutionHolder.get();
                    	LOG.info("Alias: " + flowAlias+ " - Updated execution: " + updateExecution.getId() + " - Display: " + updateExecution.getDisplayName() + " - Provider: " + updateExecution.getProviderId());
                        int distance = updateExecution.getIndex() - aer.getIndex();
                    	LOG.info("Reseting index old: " + aer.getIndex()+ " - new: " + updateExecution.getIndex() + " - Distance: " + distance);
                        for (int i = 0; i < distance; i++) {
                        	realmFlows.raisePriority(updateExecution.getId());
    					}
                    } else {
                    	String s = "Could not find created executor for Alias: " + flowAlias+ " - with Provider: " + createdExecution.getProviderId();
                    	LOG.error(s);
                    	throw new IOException(s);
                    }
    				
    			}
    		}
    		
		}
    }

    private RealmResource getRealm(String id) {
        RealmResource realm = keycloak.realms().realm(id);
        try {
            realm.toRepresentation();
            return realm;
        } catch (javax.ws.rs.NotFoundException nnfe) {
            return null;
        }
    }

    private void setFlowBindings(AuthenticationConfig authenticationConfig, RealmResource realm) {
        RealmRepresentation realmRepresentation = realm.toRepresentation();
        realmRepresentation.setBrowserFlow(authenticationConfig.getBrowserFlow());
        realmRepresentation.setDirectGrantFlow(authenticationConfig.getDirectGrantFlow());
        realm.update(realmRepresentation);
    }
}
