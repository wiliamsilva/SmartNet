package br.com.wps.smartnet.exception;

public class OpenError extends Exception {

	private static final long serialVersionUID = 4425306851373021324L;

	public OpenError(String message) {
		super(message);
	}
	
	public OpenError(String message, Throwable cause) {
		super(message, cause);
	}

}
