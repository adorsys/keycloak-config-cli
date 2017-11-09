package de.adorsys.keycloak.config.utils;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PartialUpdater {
	private static ObjectMapper mapper;
	
	static {
		mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
	}

	@SuppressWarnings("unchecked")
	public static <T> T deepCloneObject(T object) throws IOException{
		if(object==null) return null;
		String asString = mapper.writeValueAsString(object);
		return (T) mapper.readValue(asString, object.getClass());
	}

	public static <T> T deepPatchObject(T src, T dest) throws IOException{
		if(src==null) return dest;
		if(dest==null) return dest;
		
		String valueAsString = mapper.writeValueAsString(src);
		return mapper.readerForUpdating(dest).readValue(valueAsString);
	}
}
