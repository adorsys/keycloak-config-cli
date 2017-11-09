package de.adorsys.keycloak.config.realm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.representations.idm.ComponentRepresentation;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class ComponentDefinition {
    private String name;
    private String providerId;
    private String providerType;
    private ComponentDefnitionConfiguration config;
    
    private List<ComponentDefinition> children;
    
    public static class ComponentDefnitionConfiguration {
    	
    	private Map<String, String> additionalProperties = new HashMap<>();

		@JsonAnyGetter
    	public Map<String, String> getAdditionalProperties() {
    		return this.additionalProperties;
    	}
    	
    	@JsonAnySetter
    	public void setAdditionalProperty(String name, String value) {
    		this.additionalProperties.put(name, value);
    	}
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public String getProviderType() {
		return providerType;
	}

	public void setProviderType(String providerType) {
		this.providerType = providerType;
	}

	public ComponentDefnitionConfiguration getConfig() {
		return config;
	}

	public void setConfig(ComponentDefnitionConfiguration config) {
		this.config = config;
	}

	public List<ComponentDefinition> getChildren() {
		return children;
	}

	public void setChildren(List<ComponentDefinition> children) {
		this.children = children;
	}

	public ComponentRepresentation toRepresentation(String parentId) {
		ComponentRepresentation componentRepresentation = new ComponentRepresentation();
		componentRepresentation.setParentId(parentId);
		componentRepresentation.setName(getName());
		componentRepresentation.setProviderId(getProviderId());
		componentRepresentation.setProviderType(getProviderType());
		
		MultivaluedHashMap<String, String> newConfig = new MultivaluedHashMap<>();
		if (getConfig() != null) {
			getConfig().getAdditionalProperties().forEach((key, value) -> newConfig.add(key, value));
			componentRepresentation.setConfig(newConfig);
		}
		return componentRepresentation;
		
	}
    
}
