/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.arquillian.container.mobicents.servlet.sip.embedded_1;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that SIP Servlets deployments into the on Tomcat server work through the
 * Arquillian lifecycle
 * 
 * @author <a href="mailto:jean.deruelle@gmail.com">Jean Deruelle</a>
 * @author Dan Allen
 * @version $Revision: $
 */
@RunWith(Arquillian.class)
//@RunAsClient
public class MobicentsSipServletsEmbeddedClientTestCase
{
	private static final String HELLO_WORLD_URL = "http://localhost:8888/test/Test";
	
	static {
		System.setProperty("javax.servlet.sip.dar", Thread.currentThread().getContextClassLoader().getResource("test-dar.properties").toString());
        
	}
	// -------------------------------------------------------------------------------------||
	// Class Members
	// ----------------------------------------------------------------------||
	// -------------------------------------------------------------------------------------||

	/**
	 * Logger
	 */
	private static final Logger log = Logger.getLogger(MobicentsSipServletsEmbeddedClientTestCase.class.getName());

	// -------------------------------------------------------------------------------------||
	// Instance Members
	// -------------------------------------------------------------------||
	// -------------------------------------------------------------------------------------||

	/**
	 * Define the deployment
	 */
	@Deployment
	public static WebArchive createDeployment()
   {
		return ShrinkWrap.create(WebArchive.class, "test.war")
         .addClass(TestServlet.class)
         .addClass(TestSipServlet.class)
         .setWebXML("client-web.xml")
         .addAsWebInfResource("in-container-sip.xml", "sip.xml");

//		 .setSipXML("client-sip.xml");
	}

	// -------------------------------------------------------------------------------------||
	// Tests
	// ------------------------------------------------------------------------------||
	// -------------------------------------------------------------------------------------||

	/**
	 * Ensures the {@link HelloWorldServlet} returns the expected response
	 */
	@Test
	public void shouldBeAbleToInvokeServletInDeployedWebApp() throws Exception
   {
		// Define the input and expected outcome
		final String expected = "helloArquillianMobicentsSipServletsTestApplication";

		URL url = new URL(HELLO_WORLD_URL);
		InputStream in = url.openConnection().getInputStream();

		byte[] buffer = new byte[10000];
		int len = in.read(buffer);
		String httpResponse = "";
		for (int q = 0; q < len; q++)
			httpResponse += (char) buffer[q];

		// Test
		Assert.assertEquals("Expected output was not equal by value", expected, httpResponse);
		log.info("Got expected result from Http Servlet: " + httpResponse);
	}
}
