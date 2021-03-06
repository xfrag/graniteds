package org.granite.test.jmf.model;

import org.granite.messaging.annotations.Serialized;

@Serialized(propertiesOrder={ "name", "test" })
public class IllegalEmptyPropertiesOrderBean {

	private String name;
	
	public IllegalEmptyPropertiesOrderBean() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
