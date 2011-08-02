/**
 * 
 */
package org.jboss.arquillian.container.mobicents.servlet.sip.embedded_1;

import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;

/**
 * @author gvagenas 
 * gvagenas@gmail.com / devrealm.org
 * 
 */
public class SipContext extends HTTPContext {


	public SipContext(String host, int port) {
		super(host, port);
	}

}
