package de.adorsys.keycloak.password.encryption.cli.util;

import java.time.ZonedDateTime;

import net.minidev.json.JSONObject;

public class PasswordPayload {
	private String password;
	private String timestamp;
	
	
	public PasswordPayload(String password) {
		super();
		this.password = password;
		this.timestamp = ZonedDateTime.now().toString();
	}
	
	public JSONObject toJSONObject() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("pwd", password);
		jsonObject.put("timestamp", timestamp);
		return jsonObject;
	}
}
