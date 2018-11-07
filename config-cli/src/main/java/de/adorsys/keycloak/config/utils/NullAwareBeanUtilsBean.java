package de.adorsys.keycloak.config.utils;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtilsBean2;

public class NullAwareBeanUtilsBean extends BeanUtilsBean2 {

	@Override
	public void copyProperty(Object dest, String name, Object value)
			throws IllegalAccessException, InvocationTargetException {
		if (value == null)
			return;
		super.copyProperty(dest, name, value);
	}

}
