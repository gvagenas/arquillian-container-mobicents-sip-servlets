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

import java.io.IOException;
import java.util.logging.Logger;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;

import org.mobicents.servlet.sip.ctf.core.extension.event.request.Invite;


/**
 * TestSipServlet
 *
 *
 * @author gvagenas gvagenas@gmail.com / devrealm.org
 * @author Jean Deruelle
 * @version $Revision: $
 */

public class TestSipServlet // extends SipServlet
{
//	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(TestSipServlet.class.getName());

	@Inject
	SipFactory sipFactory;
	
	protected void doInvite(@Observes @Invite SipServletRequest req) throws ServletException,
	IOException {
		log.info("Got INVITE!"+req);
	}
}
