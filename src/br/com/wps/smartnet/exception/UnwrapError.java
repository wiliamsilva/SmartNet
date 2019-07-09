package br.com.wps.smartnet.exception;

public class UnwrapError extends Exception {

	private static final long serialVersionUID = 4425306851373021324L;

	public UnwrapError(String message) {
		super(message);
	}
	
	public UnwrapError(String message, Throwable cause) {
		super(message, cause);
	}

}
