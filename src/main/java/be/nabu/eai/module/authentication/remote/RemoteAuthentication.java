package be.nabu.eai.module.authentication.remote;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.WebApplicationUtils;
import be.nabu.eai.module.web.application.WebFragment;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.authentication.api.Permission;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.server.HTTPServerUtils;
import be.nabu.libs.resources.api.ResourceContainer;

public class RemoteAuthentication extends JAXBArtifact<RemoteAuthenticationConfiguration> implements WebFragment {

	private Map<String, EventSubscription<?, ?>> subscriptions = new HashMap<String, EventSubscription<?, ?>>();
	
	public RemoteAuthentication(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "remote-authentication.xml", RemoteAuthenticationConfiguration.class);
	}

	private String getKey(WebApplication artifact, String path) {
		return artifact.getId() + ":" + path;
	}
	
	@Override
	public void start(WebApplication application, String path) throws IOException {
		String key = getKey(application, path);
		if (subscriptions.containsKey(key)) {
			stop(application, path);
		}
		if (getConfig().getPath() != null && getConfig().getKeyStore() != null && getConfig().getKeyAlias() != null) {
			String originalPath = WebApplicationUtils.relativize(application.getServerPath(), path);
			String fragmentPath = WebApplicationUtils.relativize(originalPath, getConfig().getPath());
			EventSubscription<HTTPRequest, HTTPResponse> subscription = application.getConfiguration().getVirtualHost().getDispatcher().subscribe(HTTPRequest.class, new RemoteAuthenticationListener(application, this, originalPath));
			subscription.filter(HTTPServerUtils.limitToPath(fragmentPath));
			subscriptions.put(key, subscription);
		}
	}

	@Override
	public void stop(WebApplication application, String path) {
		String key = getKey(application, path);
		if (subscriptions.containsKey(key)) {
			synchronized(subscriptions) {
				if (subscriptions.containsKey(key)) {
					subscriptions.get(key).unsubscribe();
					subscriptions.remove(key);
				}
			}
		}
	}

	@Override
	public List<Permission> getPermissions(WebApplication artifact, String path) {
		return new ArrayList<Permission>();
	}
	@Override
	public boolean isStarted(WebApplication artifact, String path) {
		return false;
	}

}
