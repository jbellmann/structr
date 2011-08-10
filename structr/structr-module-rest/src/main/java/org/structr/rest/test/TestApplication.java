package org.structr.rest.test;

import com.sun.jersey.api.core.PackagesResourceConfig;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Christian Morgner
 */
public class TestApplication extends PackagesResourceConfig {

	private static final Logger logger = Logger.getLogger(TestApplication.class.getName());
	private Set<Class<?>> classes = null;
	
	public TestApplication() {
		
		classes = new LinkedHashSet<Class<?>>();
		classes.add(TestResource.class);
	}
	
	@Override
	public Set<Class<?>> getClasses() {	
		
		logger.log(Level.INFO, "Returning {0} classes", classes.size());
		
		return(classes);
	}
}
