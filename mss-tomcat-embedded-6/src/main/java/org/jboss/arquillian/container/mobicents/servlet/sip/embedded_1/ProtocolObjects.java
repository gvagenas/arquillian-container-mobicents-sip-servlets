/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.arquillian.container.mobicents.servlet.sip.embedded_1;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

import javax.sip.ObjectInUseException;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.address.AddressFactory;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;



/**
 * @author M. Ranganathan
 * 
 */
public class ProtocolObjects {
	public final AddressFactory addressFactory;

	public final MessageFactory messageFactory;

	public final HeaderFactory headerFactory;

	public final SipStack sipStack;

	private int logLevel = 32;

	String logFileDirectory = "logs/";

	public final String transport;

	private boolean isStarted;

	public ProtocolObjects(String stackname, String pathname, String transport,
			boolean autoDialog, String outboundProxy, String threadPoolSize, String reentrantListener, Map<String, String> additionalProperties) {
		this.transport = transport;
		SipFactory sipFactory = SipFactory.getInstance();
		sipFactory.resetFactory();
		sipFactory.setPathName(pathname);
		Properties properties = new Properties();
		properties.setProperty("javax.sip.STACK_NAME", stackname);

		if(outboundProxy != null) {
			properties.setProperty("javax.sip.OUTBOUND_PROXY", outboundProxy + "/"
				+ transport);
		}
		// The following properties are specific to nist-sip
		// and are not necessarily part of any other jain-sip
		// implementation.
		properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", logFileDirectory
				+ stackname + "debuglog.txt");
		properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
				logFileDirectory + stackname + "log.xml");

		properties.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT",
				(autoDialog ? "on" : "off"));

		// For the forked subscribe notify test
		properties.setProperty("javax.sip.FORKABLE_EVENTS", "foo");

		
		// Dont use the router for all requests.
//		properties.setProperty("javax.sip.USE_ROUTER_FOR_ALL_URIS", "false");

		properties.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", threadPoolSize == null ? "1" : threadPoolSize);
		properties.setProperty("gov.nist.javax.sip.REENTRANT_LISTENER", reentrantListener == null ? "false" : reentrantListener);

		// Set to 0 in your production code for max speed.
		// You need 16 for logging traces. 32 for debug + traces.
		// Your code will limp at 32 but it is best for debugging.
		properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", Integer.valueOf(
				logLevel).toString());

		if(additionalProperties != null) {
			properties.putAll(additionalProperties);
		}
		
		try {
			// Create SipStack object
			sipStack = sipFactory.createSipStack(properties);
			System.out.println("createSipStack " + sipStack);
		} catch (Exception e) {
			// could not find
			// gov.nist.jain.protocol.ip.sip.SipStackImpl
			// in the classpath
			e.printStackTrace();
			System.err.println(e.getMessage());
			throw new RuntimeException("Stack failed to initialize");
		}

		try {
			headerFactory = sipFactory.createHeaderFactory();
			addressFactory = sipFactory.createAddressFactory();
			messageFactory = sipFactory.createMessageFactory();
		} catch (SipException ex) {
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
	
	public ProtocolObjects(String stackname, String pathname, String transport,
			boolean autoDialog, String outboundProxy, String threadPoolSize, String reentrantListener) {

		this(stackname, pathname, transport, autoDialog, outboundProxy, threadPoolSize, reentrantListener, null);
	}

	public synchronized void destroy() {
		HashSet<SipProvider> hashSet = new HashSet<SipProvider>();
		
		for (SipProvider sipProvider : hashSet) {
			hashSet.add(sipProvider);
		}

		for (SipProvider sipProvider : hashSet) {
			for (int j = 0; j < 5; j++) {
				try {
					sipStack.deleteSipProvider(sipProvider);
				} catch (ObjectInUseException ex) {
					try {
						Thread.sleep(1000);
					} catch (Exception e) {
					}

				}
			}
		}

		sipStack.stop();
	}

	public void start() throws Exception {
		if (this.isStarted)
			return;
		sipStack.start();
		this.isStarted = true;

	}
}
