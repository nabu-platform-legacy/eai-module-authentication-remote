package be.nabu.eai.module.authentication.remote;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;

public class RemoteAuthenticationGUIManager extends BaseJAXBGUIManager<RemoteAuthenticationConfiguration, RemoteAuthentication> {

	public RemoteAuthenticationGUIManager() {
		super("Remote Authentication", RemoteAuthentication.class, new RemoteAuthenticationManager(), RemoteAuthenticationConfiguration.class);
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected RemoteAuthentication newInstance(MainController controller, RepositoryEntry entry, Value<?>... values) throws IOException {
		return new RemoteAuthentication(entry.getId(), entry.getContainer(), entry.getRepository());
	}
	
	@Override
	public String getCategory() {
		return "Authentication";
	}

}
