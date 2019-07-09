package br.com.wps.smartnet.core;

public abstract class Configuration {
	
	//public static final String DEFAULT_PROTOCOL = "TLSv1";
	public static final String DEFAULT_PROTOCOL = "TLSv1.2";
	//public static final String DEFAULT_PROTOCOL = "SSLv3";

	private EnumConfigurationMode mode;
	
	private boolean keepAlive;
	private int waitTimeout;
	private int idleTimeout;
	private int sendDelayByConnectionMillis;
	private int reconnectionInterval;
	private int maxSSLMessageProcess;
		
	private String trustStoreFile;
	private String trustStorePassword;
	private String keyStoreFile;
	private String keyStorePassword;
	private String keyPassword;
	
	public Configuration(EnumConfigurationMode mode) {

		this.mode = mode;
		
		this.keepAlive = false;
		this.waitTimeout = 0;
		this.idleTimeout = 0;
		this.sendDelayByConnectionMillis = 0;
		this.reconnectionInterval = 0;
		this.maxSSLMessageProcess = 0;
		
		this.trustStoreFile = null;
		this.trustStorePassword = null;
		this.keyStoreFile = null;
		this.keyStorePassword = null;
		this.keyPassword = null;

	}

	public EnumConfigurationMode getMode() {
		return mode;
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	public int getWaitTimeout() {
		return waitTimeout;
	}

	public void setWaitTimeout(int waitTimeout) {
		this.waitTimeout = waitTimeout;
	}

	public int getIdleTimeout() {
		return idleTimeout;
	}

	public void setIdleTimeout(int idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public int getSendDelayByConnectionMillis() {
		return sendDelayByConnectionMillis;
	}

	public void setSendDelayByConnectionMillis(int sendDelayByConnectionMillis) {
		this.sendDelayByConnectionMillis = sendDelayByConnectionMillis;
	}

	public int getReconnectionInterval() {
		return reconnectionInterval;
	}

	public void setReconnectionInterval(int reconnectionInterval) {
		this.reconnectionInterval = reconnectionInterval;
	}	

	public int getMaxSSLMessageProcess() {
		return maxSSLMessageProcess;
	}

	public void setMaxSSLMessageProcess(int maxSSLMessageProcess) {
		this.maxSSLMessageProcess = maxSSLMessageProcess;
	}

	public String getTrustStoreFile() {
		return trustStoreFile;
	}

	public void setTrustStoreFile(String trustStoreFile) {
		this.trustStoreFile = trustStoreFile;
	}

	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	public String getKeyStoreFile() {
		return keyStoreFile;
	}

	public void setKeyStoreFile(String keyStoreFile) {
		this.keyStoreFile = keyStoreFile;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public String getKeyPassword() {
		return keyPassword;
	}

	public void setKeyPassword(String keyPassword) {
		this.keyPassword = keyPassword;
	}

	public void setMode(EnumConfigurationMode mode) {
		this.mode = mode;
	}	

}