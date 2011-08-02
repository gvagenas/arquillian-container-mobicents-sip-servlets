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

import javax.annotation.Resource;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.sip.ConvergedHttpSession;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;

import org.mobicents.servlet.sip.weld.extension.event.request.Invite;

/**
 * TestServlet
 *
 * @author Jean Deruelle
 * @version $Revision: $
 */
public class TestServlet extends HttpServlet
{
   private static final long serialVersionUID = 1L;

   public static final String URL_PATTERN = "/Test";

   public static final String MESSAGE = "hello";
   
   private static final Logger log = Logger.getLogger(TestServlet.class.getName());
   
   @Inject
   SipFactory sipFactory;

   @Override
   protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
   {
	  ConvergedHttpSession convergedHttpSession = (ConvergedHttpSession) request.getSession();
	  SipApplicationSession applicationSession = convergedHttpSession.getApplicationSession();
	  SipServletRequest sipServletRequest = 
		  sipFactory.createRequest(applicationSession, "INVITE", "sip:jean.deruelle@127.0.0.1:5061", "sip:arquillian@127.0.0.1:5062");
//	  sipServletRequest.getSession().setHandler(TestSipServlet.class.getSimpleName());

	  sipServletRequest.send();
	  
      response.getWriter().append(MESSAGE + applicationSession.getApplicationName());
   }
   
}
