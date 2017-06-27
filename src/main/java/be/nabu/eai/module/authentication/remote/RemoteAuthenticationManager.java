package be.nabu.eai.module.authentication.remote;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class RemoteAuthenticationManager extends JAXBArtifactManager<RemoteAuthenticationConfiguration, RemoteAuthentication> {

	public RemoteAuthenticationManager() {
		super(RemoteAuthentication.class);
	}

	@Override
	protected RemoteAuthentication newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new RemoteAuthentication(id, container, repository);
	}

}
