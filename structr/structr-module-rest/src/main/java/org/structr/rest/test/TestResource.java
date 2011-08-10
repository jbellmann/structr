package org.structr.rest.test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 *
 * @author Christian Morgner
 */
@Path("/test")
@Produces("text/html")
public class TestResource {

	@GET @Produces("application/json")
	public TestBean get() {
		
		return(new TestBean(123, "HelloWord"));
	}
}
