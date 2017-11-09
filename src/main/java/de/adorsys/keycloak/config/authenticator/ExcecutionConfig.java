package de.adorsys.keycloak.config.authenticator;

import java.util.List;

public class ExcecutionConfig {
	// The new alias. We are supposed to discove the name of this alias.
	private String alias;
	
	// The 
	private String modelProvider;
	private String provider;
    private List<ExcecutionConfig> executions;
    private String displayName;

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getModelProvider() {
		return modelProvider;
	}

	public void setModelProvider(String modelProvider) {
		this.modelProvider = modelProvider;
	}

	public List<ExcecutionConfig> getExecutions() {
		return executions;
	}

	public void setExecutions(List<ExcecutionConfig> executions) {
		this.executions = executions;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
}
