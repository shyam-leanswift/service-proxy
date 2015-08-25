/* Copyright 2009, 2015 predic8 GmbH, www.predic8.com

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package com.predic8.membrane.core.interceptor.swagger;

import java.util.regex.Pattern;

import org.springframework.http.MediaType;

import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.util.Json;

import com.predic8.membrane.annot.MCAttribute;
import com.predic8.membrane.annot.MCElement;
import com.predic8.membrane.core.exchange.Exchange;
import com.predic8.membrane.core.interceptor.AbstractInterceptor;
import com.predic8.membrane.core.interceptor.Outcome;
import com.predic8.membrane.core.rules.ServiceProxy;

/**
 * @description Allow Swagger proxying
 */
@MCElement(name = "swaggerRewriter")
public class SwaggerRewriterInterceptor extends AbstractInterceptor {

	private boolean rewriteUI = true;
	private String swaggerJson = "swagger.json";

	@Override
	public Outcome handleResponse(Exchange exc) throws Exception {

		System.out.println(exc.getRequest().getUri() + " : " + exc.getRequest().getUri().matches("/.*\\.js(on)?"));

		// replacement in swagger.json
		if (exc.getRequest().getUri().endsWith(swaggerJson) && exc.getResponseContentType().equalsIgnoreCase(MediaType.APPLICATION_JSON_VALUE)) {
			Swagger swagBody = new SwaggerParser().parse(exc.getResponse().getBodyAsStringDecoded());
			swagBody.setHost(exc2originalHostPort(exc));
			exc.getResponse().setBodyContent(Json.pretty(swagBody).getBytes(exc.getResponse().getCharset()));
		}

		// replacement in json and javascript (specifically UI)
		System.out.println(exc.getResponse().getHeader().getContentType());
		if (rewriteUI &&
				(exc.getRequest().getUri().matches("/.*.js(on)?")
					|| exc.getResponse().getHeader().getContentType() != null
						&& exc.getResponse().getHeader().getContentType().equals(MediaType.TEXT_HTML_VALUE)
				)) {
			System.out.println("INSIDE");
			String from = "(http(s)?://)" + Pattern.quote(((ServiceProxy) exc.getRule()).getTarget().getHost()) + "(/.*\\.js(on)?)";
			System.out.println(from);
			String to = "$1" + exc2originalHostPort(exc) + "$3";
			System.out.println(to);
			byte[] body = exc.getResponse().getBodyAsStringDecoded().replaceAll(from, to).getBytes(exc.getResponse().getCharset());
			exc.getResponse().setBodyContent(body);
		}

		return super.handleResponse(exc);
	}

	private String exc2originalHostPort(Exchange exc) {
		return exc.getOriginalHostHeaderHost() + (exc.getOriginalHostHeaderPort().length() > 0 ? ":" + exc.getOriginalHostHeaderPort() : "");
	}

	@Override
	public String getShortDescription() {
		return super.getShortDescription();
	}

	public boolean isRewriteUI() {
		return rewriteUI;
	}
	/**
	 * @description Whether a Swagger-UI should also be rewritten.
	 * @default true
	 */
	@MCAttribute
	public void setRewriteUI(boolean rewriteUI) {
		this.rewriteUI = rewriteUI;
	}

	public String getSwaggerJson() {
		return swaggerJson;
	}
	/**
	 * @description Swagger specification filename. The default is 'swagger.json', which is also recommended.
	 * @default swagger.json
	 */
	@MCAttribute
	public void setSwaggerJson(String swaggerJson) {
		this.swaggerJson = swaggerJson;
	}

}
