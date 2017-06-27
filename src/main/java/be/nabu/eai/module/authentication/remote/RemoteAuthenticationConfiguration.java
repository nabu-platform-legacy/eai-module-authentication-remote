package be.nabu.eai.module.authentication.remote;

import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.module.keystore.KeyStoreArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;

@XmlRootElement(name = "remoteAuthentication")
public class RemoteAuthenticationConfiguration {
	
	private String path, errorPath, successPath;
	
	private String keyAlias;
	
	private KeyStoreArtifact keyStore;

	@NotNull
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}

	@NotNull
	@EnvironmentSpecific
	public String getKeyAlias() {
		return keyAlias;
	}
	public void setKeyAlias(String keyAlias) {
		this.keyAlias = keyAlias;
	}

	@NotNull
	@EnvironmentSpecific
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public KeyStoreArtifact getKeyStore() {
		return keyStore;
	}
	public void setKeyStore(KeyStoreArtifact keyStore) {
		this.keyStore = keyStore;
	}
	
	public String getErrorPath() {
		return errorPath;
	}
	public void setErrorPath(String errorPath) {
		this.errorPath = errorPath;
	}
	
	public String getSuccessPath() {
		return successPath;
	}
	public void setSuccessPath(String successPath) {
		this.successPath = successPath;
	}
	
}
