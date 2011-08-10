package org.structr.rest.test;

import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Christian Morgner
 */
@XmlRootElement
public class TestBean {
	
	private long id = -1;
	private String name = null;
	
	public TestBean() {
	}

	public TestBean(long id, String name) {
		
		this.id = id;
		this.name = name;
	}
	
	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	
}
