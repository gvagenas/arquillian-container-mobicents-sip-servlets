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

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.ExpandWar;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.jboss.shrinkwrap.mobicents.servlet.sip.api.ShrinkWrapSipStandardContext;
import org.mobicents.servlet.sip.SipConnector;
import org.mobicents.servlet.sip.core.SipApplicationDispatcherImpl;
import org.mobicents.servlet.sip.core.session.SipStandardManager;
import org.mobicents.servlet.sip.startup.SipStandardContext;
import org.mobicents.servlet.sip.startup.SipStandardService;

/**
 * <p>Arquillian {@link DeployableContainer} implementation for an
 * Embedded Tomcat server; responsible for both lifecycle and deployment
 * operations.</p>
 *
 * <p>Please note that the context path set for the webapp must begin with
 * a forward slash. Otherwise, certain path operations within Tomcat
 * will behave inconsistently. Though it goes without saying, the host
 * name (bindAddress) cannot have a trailing slash for the same
 * reason.</p>
 * 
 * @author <a href="mailto:jean.deruelle@gmail.com">Jean Deruelle</a>
 * @author Dan Allen
 * @version $Revision: $
 */
public class MobicentsSipServletsContainer implements DeployableContainer<MobicentsSipServletsConfiguration>
{
	private static final Logger log = Logger.getLogger(MobicentsSipServletsContainer.class.getName());   

	private static final String ENV_VAR = "${env.";

	private static final String TMPDIR_SYS_PROP = "java.io.tmpdir";

	private static final String SIP_PROTOCOL = "sip";

	protected List<SipConnector> sipConnectors;   

	/**
	 * Tomcat embedded
	 */
	private MobicentsSipServletsEmbedded embedded;

	/**
	 * Engine contained within Tomcat embedded
	 */
	private Engine engine;

	/**
	 * Host contained in the tomcat engine
	 */
	private Host standardHost;

	/**
	 * Tomcat container configuration
	 */
	private MobicentsSipServletsConfiguration configuration;

	private String serverName;

	private String bindAddress;

	private int bindPort;

	private boolean wasStarted;

	@Inject @DeploymentScoped 
	private InstanceProducer<SipStandardContext> sipStandardContextProducer;

	@Override
	public Class<MobicentsSipServletsConfiguration> getConfigurationClass() {
		return MobicentsSipServletsConfiguration.class;
	}

	@Override
	public ProtocolDescription getDefaultProtocol()
	{
		return new ProtocolDescription("Servlet 2.5");
	}

	@Override
	public void setup(MobicentsSipServletsConfiguration configuration)
	{
		this.configuration = (MobicentsSipServletsConfiguration) configuration;
		bindAddress = this.configuration.getBindAddress();
		bindPort = this.configuration.getBindHttpPort();
		sipConnectors = getSipConnectors(this.configuration.getSipConnectors());
		serverName = this.configuration.getServerName();
	}

	protected List<SipConnector> getSipConnectors(String sipConnectorString) {
		List<SipConnector> connectors = new ArrayList<SipConnector>();

		StringTokenizer tokenizer = new StringTokenizer(sipConnectorString, ",");
		while (tokenizer.hasMoreTokens()) {
			String connectorString = tokenizer.nextToken();
			String bindSipAddress;
			int bindSipPort;
			String bindSipTransport;

			int indexOfColumn = connectorString.indexOf(":");
			int indexOfSlash = connectorString.indexOf("/");
			if(indexOfColumn == -1) {
				throw new IllegalArgumentException("sipConnectors configuration should be a comma separated list of <ip_address>:<port>/<transport>");
			}
			if(indexOfColumn == 0) {
				bindSipAddress = this.bindAddress;
			} else {
				bindSipAddress = connectorString.substring(0,indexOfColumn);
			}
			if(indexOfSlash != -1) {
				bindSipPort = Integer.parseInt(connectorString.substring(indexOfColumn + 1, indexOfSlash));
				bindSipTransport = connectorString.substring(indexOfSlash + 1);
			} else {
				bindSipPort = Integer.parseInt(connectorString.substring(indexOfColumn + 1));
				bindSipTransport = "UDP";
			}
			SipConnector sipConnector = new SipConnector();
			sipConnector.setIpAddress(bindSipAddress);
			sipConnector.setPort(bindSipPort);
			try {
				sipConnector.setTransport(bindSipTransport);
			} catch (Exception e) {}
			connectors.add(sipConnector);
		} 

		return connectors;
	}

	protected void startTomcatEmbedded() throws UnknownHostException, org.apache.catalina.LifecycleException, LifecycleException
	{
		
		  System.setProperty("javax.servlet.sip.ar.spi.SipApplicationRouterProvider", configuration.getSipApplicationRouterProviderClassName());
		  if(MobicentsSipServletsConfiguration.MOBICENTS_DEFAULT_AR_CLASS_NAME.equals(configuration.getSipApplicationRouterProviderClassName())) {
			  System.setProperty("javax.servlet.sip.dar", Thread.currentThread().getContextClassLoader().getResource("empty-dar.properties").toString());
		  } else {
			  System.setProperty("javax.servlet.sip.dar", Thread.currentThread().getContextClassLoader().getResource("test-dar.properties").toString());
		  }
	      // creating the tomcat embedded == service tag in server.xml
		  MobicentsSipServletsEmbedded embedded= new MobicentsSipServletsEmbedded();
		  this.embedded = embedded;
	      SipStandardService sipStandardService = new SipStandardService();
	      sipStandardService.setSipApplicationDispatcherClassName(SipApplicationDispatcherImpl.class.getCanonicalName());
	      sipStandardService.setCongestionControlCheckingInterval(-1);
	      sipStandardService.setAdditionalParameterableHeaders("additionalParameterableHeader");
	      sipStandardService.setUsePrettyEncoding(true);      
	      sipStandardService.setName(serverName);
	      embedded.setService(sipStandardService);
	      // TODO this needs to be a lot more robust
	      String tomcatHome = configuration.getTomcatHome();
	      File tomcatHomeFile = null;
	      if (tomcatHome != null)
	      {
	         if (tomcatHome.startsWith(ENV_VAR))
	         {
	            String sysVar = tomcatHome.substring(ENV_VAR.length(), tomcatHome.length() - 1);
	            tomcatHome = System.getProperty(sysVar);
	            if (tomcatHome != null && tomcatHome.length() > 0 && new File(tomcatHome).isAbsolute())
	            {
	               tomcatHomeFile = new File(tomcatHome);
	               log.info("Using tomcat home from environment variable: " + tomcatHome);
	            }
	         }
	         else
	         {
	            tomcatHomeFile = new File(tomcatHome);
	         }
	      }

	      if (tomcatHomeFile == null)
	      {
	         tomcatHomeFile = new File(System.getProperty(TMPDIR_SYS_PROP), "mss-tomcat-embedded-6");
	      }

	      tomcatHomeFile.mkdirs();
	      embedded.setCatalinaBase(tomcatHomeFile.getAbsolutePath());
	      embedded.setCatalinaHome(tomcatHomeFile.getAbsolutePath());
	     
	      // creates the engine, i.e., <engine> element in server.xml
	      Engine engine = embedded.createEngine();
	      this.engine = engine;
	      engine.setName(serverName);
	      engine.setDefaultHost(bindAddress);
	      engine.setService(sipStandardService);
	      sipStandardService.setContainer(engine);
	      embedded.addEngine(engine);
	      
	      // creates the host, i.e., <host> element in server.xml
	      File appBaseFile = new File(tomcatHomeFile, configuration.getAppBase());
	      appBaseFile.mkdirs();
	      StandardHost host = (StandardHost) embedded.createHost(bindAddress, appBaseFile.getAbsolutePath());
	      this.standardHost = host;
	      if (configuration.getTomcatWorkDir() != null)
	      {
	         host.setWorkDir(configuration.getTomcatWorkDir());
	      }
	      host.setUnpackWARs(configuration.isUnpackArchive());
	      engine.addChild(host);
	      
	      // creates an http connector, i.e., <connector> element in server.xml
	      Connector connector = embedded.createConnector(InetAddress.getByName(bindAddress), bindPort, false);
	      embedded.addConnector(connector);
	      connector.setContainer(engine);
	      
	      // starts embedded Mobicents Sip Serlvets
	      embedded.start();
	      embedded.getService().start();
	      
	   // creates an sip connector, i.e., <connector> element in server.xml
	      for (SipConnector sipConnector : sipConnectors) {
	    	  try {
	    		  (sipStandardService).addSipConnector(sipConnector);
//	    			Connector sipConnector = addSipConnector(serverName, bindAddress, bindSipPort, "UDP", null);
	    		} catch (Exception e) {
	    			throw new LifecycleException("Couldn't create the sip connector " + sipConnector, e);
	    		}
	      }
	      
	      wasStarted = true;
//	      return embedded;
		
//		System.setProperty("javax.servlet.sip.ar.spi.SipApplicationRouterProvider", configuration.getSipApplicationRouterProviderClassName());
//		if(MobicentsSipServletsConfiguration.MOBICENTS_DEFAULT_AR_CLASS_NAME.equals(configuration.getSipApplicationRouterProviderClassName())) {
//			System.setProperty("javax.servlet.sip.dar", Thread.currentThread().getContextClassLoader().getResource("empty-dar.properties").toString());
//		}
//		// creating the tomcat embedded == service tag in server.xml
//		MobicentsSipServletsEmbedded embedded= new MobicentsSipServletsEmbedded();
//		this.embedded = embedded;
//
//		SipStandardService sipStandardService = new SipStandardService();
//		sipStandardService.setSipApplicationDispatcherClassName(SipApplicationDispatcherImpl.class.getCanonicalName());
//		sipStandardService.setCongestionControlCheckingInterval(-1);
//		sipStandardService.setAdditionalParameterableHeaders("additionalParameterableHeader");
//		sipStandardService.setUsePrettyEncoding(true);      
//		sipStandardService.setName(serverName);
//
//		//      embedded.setService(sipStandardService);
//
//		// TODO this needs to be a lot more robust
//		String tomcatHome = configuration.getTomcatHome();
//		File tomcatHomeFile = null;
//		if (tomcatHome != null)
//		{
//			if (tomcatHome.startsWith(ENV_VAR))
//			{
//				String sysVar = tomcatHome.substring(ENV_VAR.length(), tomcatHome.length() - 1);
//				tomcatHome = System.getProperty(sysVar);
//				if (tomcatHome != null && tomcatHome.length() > 0 && new File(tomcatHome).isAbsolute())
//				{
//					tomcatHomeFile = new File(tomcatHome);
//					log.info("Using tomcat home from environment variable: " + tomcatHome);
//				}
//			}
//			else
//			{
//				tomcatHomeFile = new File(tomcatHome);
//			}
//		}
//
//		if (tomcatHomeFile == null)
//		{
//			tomcatHomeFile = new File(System.getProperty(TMPDIR_SYS_PROP), "tomcat-embedded-6");
//		}
//
//		tomcatHomeFile.mkdirs();
//		embedded.setCatalinaBase(tomcatHomeFile.getAbsolutePath());
//		embedded.setCatalinaHome(tomcatHomeFile.getAbsolutePath());
//
//		// creates the engine, i.e., <engine> element in server.xml
//		Engine engine = embedded.createEngine();
//		this.engine = engine;
//		engine.setName(serverName);
//		engine.setDefaultHost(bindAddress);
//		engine.setService(sipStandardService);
//		sipStandardService.setContainer(engine);
//		embedded.addEngine(engine);
//
//		// creates the host, i.e., <host> element in server.xml
//		File appBaseFile = new File(tomcatHomeFile, configuration.getAppBase());
//		appBaseFile.mkdirs();
//		StandardHost host = (StandardHost) embedded.createHost(bindAddress, appBaseFile.getAbsolutePath());
//		this.standardHost = host;
//		if (configuration.getTomcatWorkDir() != null)
//		{
//			host.setWorkDir(configuration.getTomcatWorkDir());
//		}
//		host.setUnpackWARs(configuration.isUnpackArchive());
//		engine.addChild(host);
//
//		// creates an http connector, i.e., <connector> element in server.xml
//		Connector connector = embedded.createConnector(InetAddress.getByName(bindAddress), bindPort, false);
//		embedded.addConnector(connector);
//		connector.setContainer(engine);
//
//		// starts embedded Mobicents Sip Serlvets
//		embedded.start();
//		//      embedded.getService().start();
//
//		// creates an sip connector, i.e., <connector> element in server.xml
//		for (SipConnector sipConnector : sipConnectors) {
//			try {
//				(sipStandardService).addSipConnector(sipConnector);
//				//    			Connector sipConnector = addSipConnector(serverName, bindAddress, bindSipPort, "UDP", null);
//			} catch (Exception e) {
//				throw new org.apache.catalina.LifecycleException("Couldn't create the sip connector " + sipConnector, e);
//			}
//		}
//
//		wasStarted = true;
//		//      return embedded;
	}

	protected void stopTomcatEmbedded() throws org.jboss.arquillian.container.spi.client.container.LifecycleException, org.apache.catalina.LifecycleException
	{
		embedded.stop();
	}

	/**
	 * Make sure an the unpacked WAR is not left behind
	 * you would think Tomcat would cleanup an unpacked WAR, but it doesn't
	 */
	protected void deleteUnpackedWAR(StandardContext standardContext)
	{
		File unpackDir = new File(standardHost.getAppBase(), standardContext.getPath().substring(1));
		if (unpackDir.exists())
		{
			ExpandWar.deleteDir(unpackDir);
		}
	}

	@Override
	public ProtocolMetaData deploy(final Archive<?> archive) throws org.jboss.arquillian.container.spi.client.container.DeploymentException
	{
		try
		{
			SipStandardContext sipStandardContext = archive.as(ShrinkWrapSipStandardContext.class);
			sipStandardContext.setXmlNamespaceAware(true);
			sipStandardContext.setManager(new SipStandardManager());
			sipStandardContext.addLifecycleListener(new EmbeddedContextConfig());
			sipStandardContext.setUnpackWAR(configuration.isUnpackArchive());
			sipStandardContext.setJ2EEServer("Arquillian-" + UUID.randomUUID().toString());

			// Need to tell TomCat to use TCCL as parent, else the WebContextClassloader will be looking in AppCL 
			sipStandardContext.setParentClassLoader(Thread.currentThread().getContextClassLoader());

			if (sipStandardContext.getUnpackWAR())
			{
				deleteUnpackedWAR(sipStandardContext);
			}

			// Override the default Tomcat WebappClassLoader, it delegates to System first. Half our testable app is on System classpath.
			WebappLoader webappLoader = new WebappLoader(sipStandardContext.getParentClassLoader());
			webappLoader.setDelegate(sipStandardContext.getDelegate());
			webappLoader.setLoaderClass(EmbeddedWebappClassLoader.class.getName());
			sipStandardContext.setLoader(webappLoader);

			standardHost.addChild(sipStandardContext);

			sipStandardContextProducer.set(sipStandardContext);

			String contextPath = sipStandardContext.getPath();
			SipContext sipContext = new SipContext(bindAddress, bindPort);

			for(String mapping : sipStandardContext.findServletMappings())
			{
				sipContext.add(new Servlet(
						sipStandardContext.findServletMapping(mapping), contextPath));
			}

			return new ProtocolMetaData()
			.addContext(sipContext);
		}
		catch (Exception e)
		{
			throw new org.jboss.arquillian.container.spi.client.container.DeploymentException("Failed to deploy " + archive.getName(), e);
		}
	}

	@Override
	public void start() throws LifecycleException {
		try
		{
			startTomcatEmbedded();
		}
		catch (Exception e)
		{
			throw new LifecycleException("Bad shit happened", e);
		}
	}

	@Override
	public void stop() throws LifecycleException {
		try
		{
//			removeFailedUnDeployments();
		}
		catch (Exception e)
		{
			throw new LifecycleException("Could not clean up", e);
		}
		if (wasStarted)
		{
			try
			{
				stopTomcatEmbedded();
			}
			catch (org.apache.catalina.LifecycleException e)
			{
				throw new LifecycleException("An unexpected error occurred", e);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.jboss.arquillian.container.spi.client.container.DeployableContainer#undeploy(org.jboss.shrinkwrap.api.Archive)
	 */
	@Override
	public void undeploy(Archive<?> archive) throws DeploymentException {
	      SipStandardContext sipStandardContext = sipStandardContextProducer.get();
	      if (sipStandardContext != null)
	      {
	         standardHost.removeChild(sipStandardContext);
	         if (sipStandardContext.getUnpackWAR())
	         {
	            deleteUnpackedWAR(sipStandardContext);
	         }
	      }
	}

	/* (non-Javadoc)
	 * @see org.jboss.arquillian.container.spi.client.container.DeployableContainer#deploy(org.jboss.shrinkwrap.descriptor.api.Descriptor)
	 */
	@Override
	public void deploy(Descriptor descriptor) throws DeploymentException {
		throw new UnsupportedOperationException("Not implemented");
	}

	/* (non-Javadoc)
	 * @see org.jboss.arquillian.container.spi.client.container.DeployableContainer#undeploy(org.jboss.shrinkwrap.descriptor.api.Descriptor)
	 */
	@Override
	public void undeploy(Descriptor descriptor) throws DeploymentException {
		throw new UnsupportedOperationException("Not implemented");
	}

	//	@Override
	//	public StandardContext createStandardContext(Context context, Archive<?> archive) throws DeploymentException {
	//		try
	//	      {
	//	         SipStandardContext sipStandardContext = archive.as(ShrinkWrapSipStandardContext.class);
	//	         sipStandardContext.setXmlNamespaceAware(true);
	//	         sipStandardContext.setManager(new SipStandardManager());
	//	         sipStandardContext.addLifecycleListener(new EmbeddedContextConfig());
	//	         sipStandardContext.setUnpackWAR(configuration.isUnpackArchive());
	//	         if (sipStandardContext.getUnpackWAR())
	//	         {
	//	            deleteUnpackedWAR(sipStandardContext);
	//	         }
	//
	//	         getStandardHost().addChild(sipStandardContext);
	//	         context.add(SipStandardContext.class, sipStandardContext);
	//	         return sipStandardContext;
	//	      }
	//	      catch (Exception e)
	//	      {
	//	         throw new DeploymentException("Failed to deploy " + archive.getName(), e);
	//	      }	      
	//	}
	//	
	//	public TomcatConfiguration getConfiguration() {
	//		return configuration;
	//	}
}
