package br.com.wps.smartnet.exception;

public class ServerError extends Exception {

	private static final long serialVersionUID = 4425306851373021324L;

	public ServerError(String message) {
		super(message);
	}
	
	public ServerError(String message, Throwable cause) {
		super(message, cause);
	}

}
