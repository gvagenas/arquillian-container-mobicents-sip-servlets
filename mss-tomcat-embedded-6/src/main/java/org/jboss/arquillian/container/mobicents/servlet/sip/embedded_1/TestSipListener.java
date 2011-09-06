/**
 * 
 */
package org.jboss.arquillian.container.mobicents.servlet.sip.embedded_1;

import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipListener;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.message.Request;

/**
 * @author gvagenas 
 * gvagenas@gmail.com / devrealm.org
 * 
 */
public class TestSipListener implements SipListener {

	private Boolean inviteReceived;
	
	@Override
	public void processDialogTerminated(DialogTerminatedEvent arg0) {

	}

	@Override
	public void processIOException(IOExceptionEvent arg0) {

	}

	@Override
	public void processRequest(RequestEvent requestEvent) {
		Request request = requestEvent.getRequest();
		
		if (request.getMethod().equals(Request.INVITE)){
			inviteReceived = true;
		}
	}

	@Override
	public void processResponse(ResponseEvent arg0) {

	}

	@Override
	public void processTimeout(TimeoutEvent arg0) {

	}

	@Override
	public void processTransactionTerminated(TransactionTerminatedEvent arg0) {

	}

	public void sendSipRequest(){}
	
}
