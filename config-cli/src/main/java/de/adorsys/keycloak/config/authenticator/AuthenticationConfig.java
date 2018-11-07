package de.adorsys.keycloak.config.authenticator;

import java.util.List;

public class AuthenticationConfig {
	
	private String browserFlow;
	private String directGrantFlow;

	private List<FlowCopyConfig> flows;

	public List<FlowCopyConfig> getFlows() {
		return flows;
	}

	public void setFlows(List<FlowCopyConfig> flows) {
		this.flows = flows;
	}

	public String getBrowserFlow() {
		return browserFlow;
	}

	public void setBrowserFlow(String browserFlow) {
		this.browserFlow = browserFlow;
	}

	public String getDirectGrantFlow() {
		return directGrantFlow;
	}

	public void setDirectGrantFlow(String directGrantFlow) {
		this.directGrantFlow = directGrantFlow;
	}
}
