/* Copyright 2009, 2012 predic8 GmbH, www.predic8.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package com.predic8.membrane.core.transport.http;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.predic8.membrane.core.exchange.Exchange;
import com.predic8.membrane.core.http.Request;
import com.predic8.membrane.core.interceptor.InterceptorFlowController;
import com.predic8.membrane.core.transport.Transport;
import com.predic8.membrane.core.util.EndOfStreamException;
import com.predic8.membrane.core.util.HttpUtil;

public abstract class AbstractHttpHandler  {

	private static Log log = LogFactory.getLog(AbstractHttpHandler.class.getName());

	protected Exchange exchange;
	protected Request srcReq;
	private static final InterceptorFlowController flowController = new InterceptorFlowController();
		
	private final Transport transport;
	
	public AbstractHttpHandler(Transport transport) {
		this.transport = transport;
	}

	public Transport getTransport() {
		return transport;
	}

	public abstract void shutdownInput() throws IOException;
	public abstract InetAddress getRemoteAddress() throws IOException;
	public abstract int getLocalPort();

	
	protected void invokeHandlers() throws IOException, EndOfStreamException, AbortException, NoMoreRequestsException, ErrorReadingStartLineException {
		try {
			flowController.invokeHandlers(exchange, transport.getInterceptors());
			if (exchange.getResponse() == null)
				throw new AbortException("No response was generated by the interceptor chain.");
		} catch (Exception e) {
			if (exchange.getResponse() == null) {
				String msg;
				boolean printStackTrace = transport.isPrintStackTrace();
				if (printStackTrace) {
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					msg = sw.toString();
				} else {
					msg = e.toString();
				}
				exchange.setResponse(HttpUtil.createHTMLErrorResponse(msg,
					"Stack traces can be " + (printStackTrace ? "dis" : "en") + "abled by setting the "+
					"@printStackTrace attribute on <a href=\"http://membrane-soa.org/esb-doc/current/configuration/reference/transport.htm\">transport</a>. " +
					"More details might be found in the log."
						));
			}
			
			if (e instanceof IOException)
				throw (IOException)e;
			if (e instanceof EndOfStreamException)
				throw (EndOfStreamException)e;
			if (e instanceof AbortException)
				throw (AbortException)e; // TODO: migrate catch logic into this method
			if (e instanceof NoMoreRequestsException)
				throw (NoMoreRequestsException)e;
			if (e instanceof ErrorReadingStartLineException)
				throw (ErrorReadingStartLineException)e;
			log.warn("An exception occured while handling a request: ", e);
		}
	}

}
