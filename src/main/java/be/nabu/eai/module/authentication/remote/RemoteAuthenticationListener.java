/*
* Copyright (C) 2017 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.authentication.remote;

import java.net.URI;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.WebApplicationUtils;
import be.nabu.libs.authentication.impl.ImpersonateToken;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.server.Session;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.http.jwt.JWTBody;
import be.nabu.libs.http.jwt.JWTToken;
import be.nabu.libs.http.jwt.JWTUtils;
import be.nabu.libs.resources.URIUtils;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.PlainMimeEmptyPart;

public class RemoteAuthenticationListener implements EventHandler<HTTPRequest, HTTPResponse> {

	private static Logger logger = LoggerFactory.getLogger(RemoteAuthenticationListener.class);
	private WebApplication application;
	private Key key;
	private RemoteAuthentication artifact;
	private String originalPath;
	
	public RemoteAuthenticationListener(WebApplication application, RemoteAuthentication artifact, String originalPath) {
		this.application = application;
		this.artifact = artifact;
		this.originalPath = originalPath;
		this.getKey();
	}
	
	private Key getKey() {
		if (key == null) {
			String keyAlias = artifact.getConfig().getKeyAlias();
			KeyStoreArtifact keystore = artifact.getConfig().getKeyStore();
			try {
				key = keystore.getKeyStore().getCertificate(keyAlias).getPublicKey();
			}
			catch (Exception e) {
				// ignore
			}
			try {
				key = keystore.getKeyStore().getChain(keyAlias)[0].getPublicKey();
			}
			catch (Exception e) {
				// ignore
			}
			try {
				key = keystore.getKeyStore().getSecretKey(keyAlias);
			}
			catch (Exception e) {
				// ignore
			}
			if (key == null) {
				throw new IllegalArgumentException("Could not retrieve " + keyAlias + " from " + keystore.getId());
			}
		}
		return key;
	}

	@Override
	public HTTPResponse handle(HTTPRequest request) {
		try {
			boolean secure = application.getConfiguration().getVirtualHost().getServer().isSecure();
			URI uri = HTTPUtils.getURI(request, secure);
			Map<String, List<String>> queryProperties = URIUtils.getQueryProperties(uri);
			List<String> token = queryProperties.get("token");
			if (token == null || token.isEmpty()) {
				throw new HTTPException(400, "No token");
			}
			JWTBody decode = JWTUtils.decode(getKey(), token.get(0));
			Session existingSession = application.getSessionResolver().getSession(request.getContent().getHeaders());
			// create a new session
			Session newSession = application.getSessionProvider().newSession();
			// copy & destroy the old one (if any)
			if (existingSession != null) {
				for (String key : existingSession) {
					newSession.set(key, existingSession.get(key));
				}
				existingSession.destroy();
			}
			// set the token in the session
			JWTToken jwtToken = new JWTToken(decode);
			if (jwtToken.getValidUntil().before(new Date())) {
				throw new HTTPException(400, "Expired");
			}
			ImpersonateToken impersonateToken = new ImpersonateToken(jwtToken, application.getRealm(), jwtToken.getName());
			newSession.set(GlueListener.buildTokenName(application.getRealm()), impersonateToken);
			List<Header> responseHeaders = new ArrayList<Header>();
			// set the correct headers to update the session
			responseHeaders.add(HTTPUtils.newSetCookieHeader(GlueListener.SESSION_COOKIE, newSession.getId(), null, application.getCookiePath(), null, secure, true));
			responseHeaders.add(new MimeHeader("Location", WebApplicationUtils.relativize(originalPath, artifact.getConfiguration().getSuccessPath())));
			responseHeaders.add(new MimeHeader("Content-Length", "0"));
			logger.debug("Sending back 307");
			return new DefaultHTTPResponse(request, 307, HTTPCodes.getMessage(307),
				new PlainMimeEmptyPart(null, responseHeaders.toArray(new Header[responseHeaders.size()]))
			);
		}
		catch (Exception e) {
			logger.error("Failed remote authentication", e);
			return new DefaultHTTPResponse(request, 307, HTTPCodes.getMessage(307),
				new PlainMimeEmptyPart(null, new MimeHeader("Content-Length", "0"),
					new MimeHeader("Location", WebApplicationUtils.relativize(originalPath, artifact.getConfig().getErrorPath()))
				));
		}
		catch(Error e) {
			logger.error("Failed remote authentication", e);
			throw e;
		}
	}
	
}
