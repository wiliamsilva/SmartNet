package br.com.wps.smartnet.exception;

public class ConnectorError extends Exception {

	private static final long serialVersionUID = 1905471182018904563L;

	public ConnectorError(String message) {
		super(message);
	}
	
	public ConnectorError(String message, Throwable cause) {
		super(message, cause);
	}

}
