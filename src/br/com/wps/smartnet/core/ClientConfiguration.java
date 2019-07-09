package br.com.wps.smartnet.core;

public class ClientConfiguration extends Configuration {

	private int connectAttempt;
	private boolean autoConnect;
	private int autoConnectInterval;
	
	private EnumSelectorConnectorStrategy selectorStrategy;
	
	public ClientConfiguration() {
	
		super(EnumConfigurationMode.Client);
		
		this.connectAttempt = 0;
		this.autoConnect = false;
		this.autoConnectInterval = 0;
		
		selectorStrategy = EnumSelectorConnectorStrategy.Default;

	}
	
	public int getConnectAttempt() {
		return connectAttempt;
	}

	public void setConnectAttempt(int connectAttempt) {
		this.connectAttempt = connectAttempt;
	}

	public boolean isAutoConnect() {
		return autoConnect;
	}

	public void setAutoConnect(boolean autoConnect) {
		this.autoConnect = autoConnect;
	}

	public int getAutoConnectInterval() {
		return autoConnectInterval;
	}

	public void setAutoConnectInterval(int autoConnectInterval) {
		this.autoConnectInterval = autoConnectInterval;
	}

	public EnumSelectorConnectorStrategy getSelectorStrategy() {
		return selectorStrategy;
	}

	public void setSelectorStrategy(EnumSelectorConnectorStrategy selectorStrategy) {
		this.selectorStrategy = selectorStrategy;
	}

}
