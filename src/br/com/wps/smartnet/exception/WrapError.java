package br.com.wps.smartnet.exception;

public class WrapError extends Exception {

	private static final long serialVersionUID = 4425306851373021324L;

	public WrapError(String message) {
		super(message);
	}
	
	public WrapError(String message, Throwable cause) {
		super(message, cause);
	}

}
