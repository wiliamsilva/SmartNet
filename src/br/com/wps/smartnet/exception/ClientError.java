package br.com.wps.smartnet.exception;

public class ClientError extends Exception {

	private static final long serialVersionUID = 4425306851373021324L;

	public ClientError(String message) {
		super(message);
	}
	
	public ClientError(String message, Throwable cause) {
		super(message, cause);
	}

}
