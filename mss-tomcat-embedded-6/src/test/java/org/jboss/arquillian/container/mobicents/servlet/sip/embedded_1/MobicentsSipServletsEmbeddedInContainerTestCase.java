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
import java.util.TooManyListenersException;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.sip.SipProvider;
import javax.sip.address.SipURI;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.GenericArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mobicents.servlet.sip.startup.SipContext;

/**
 * Tests that SIP Servlets deployments into the  Mobicents Sip Servlets server work through the
 * Arquillian lifecycle
 * 
 * This test is currently commented since the Weld integration in Mobicents Sip Servlets has not yet been done
 * 
 * @author gvagenas@gmail.com / devrealm.org
 * @author Jean Deruelle
 * @version $Revision: $
 */
@RunWith(Arquillian.class)
public class MobicentsSipServletsEmbeddedInContainerTestCase
{
	private static final String HELLO_WORLD_URL = "http://localhost:8888/test2/Test";

	// -------------------------------------------------------------------------------------||
	// Class Members
	// ----------------------------------------------------------------------||
	// -------------------------------------------------------------------------------------||

	/**
	 * Logger
	 */
	private static final Logger log = Logger.getLogger(MobicentsSipServletsEmbeddedInContainerTestCase.class.getName());

	// -------------------------------------------------------------------------------------||
	// Instance Members
	// -------------------------------------------------------------------||
	// -------------------------------------------------------------------------------------||	

	/**
	 * Define the deployment
	 */
	@Deployment
	public static WebArchive createTestArchive()
	{
		
		WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "test2.war");
		webArchive.addClasses(TestServlet.class, TestBean.class, TestSipServlet.class);
		webArchive.addAsLibraries(DependencyResolvers.use(MavenDependencyResolver.class)
//						.artifact("org.mobicents.servlet.sip.weld:sip-servlets-weld:1.0.0.ALPHA1").resolveAs(GenericArchive.class));
				.artifact("org.mobicents.servlet.sip.ctf.core:ctf-core:1.0.0-SNAPSHOT").resolveAs(GenericArchive.class));
		webArchive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
		webArchive.addAsManifestResource("in-container-context.xml", "context.xml");
		webArchive.addAsWebInfResource("in-container-web.xml", "web.xml");
		webArchive.addAsWebInfResource("in-container-sip.xml", "sip.xml");
		
		return webArchive;
	}
	
	// -------------------------------------------------------------------------------------||
	// Tests
	// ------------------------------------------------------------------------------||
	// -------------------------------------------------------------------------------------||

	@Resource(name = "name") String name;

	@Inject TestBean testBean;

	
	/**
	 * Ensures the {@link HelloWorldServlet} returns the expected response
	 */
	@Test
	public void shouldBeAbleToInjectMembersIntoTestClass()
	{
		log.info("Name: " + name);
		Assert.assertEquals("Tomcat", name);
		Assert.assertNotNull(testBean);
		Assert.assertEquals("Tomcat", testBean.getName());
	}

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
		{
			httpResponse += (char) buffer[q];
		}

		// Test
		Assert.assertEquals("Expected output was not equal by value", expected, httpResponse);
		log.info("Got expected result from Http Servlet: " + httpResponse);
	}
	
}
