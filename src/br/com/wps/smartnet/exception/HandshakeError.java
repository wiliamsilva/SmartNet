package br.com.wps.smartnet.exception;

public class HandshakeError extends Exception {

	private static final long serialVersionUID = 4425306851373021324L;

	public HandshakeError(String message) {
		super(message);
	}
	
	public HandshakeError(String message, Throwable cause) {
		super(message, cause);
	}

}
