package br.com.wps.smartnet.core;

public class ServerConfiguration extends Configuration {

	private int maxConnections;
	private int firstMessageTimeout;
	
	public ServerConfiguration() {

		super(EnumConfigurationMode.Server);

		this.maxConnections = 0;
		this.firstMessageTimeout = 0;

	}

	public int getMaxConnections() {
		return maxConnections;
	}
	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}
	public int getFirstMessageTimeout() {
		return firstMessageTimeout;
	}
	public void setFirstMessageTimeout(int firstMessageTimeout) {
		this.firstMessageTimeout = firstMessageTimeout;
	}
	
	
}
