package de.adorsys.keycloak.config.authenticator;

import java.util.List;

public class FlowCopyConfig {
	
	// Alias of the new flow to copy
	private String alias;

	// Alias of the model flow. Must be existent 
	private String modelAlias;
	
	private List<ExcecutionConfig> executions;

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getModelAlias() {
		return modelAlias;
	}

	public void setModelAlias(String modelAlias) {
		this.modelAlias = modelAlias;
	}

	public List<ExcecutionConfig> getExecutions() {
		return executions;
	}

	public void setExecutions(List<ExcecutionConfig> executions) {
		this.executions = executions;
	}
}
