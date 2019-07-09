package br.com.wps.smartnet.exception;

public class BindError extends Exception {

	private static final long serialVersionUID = 4425306851373021324L;

	public BindError(String message) {
		super(message);
	}
	
	public BindError(String message, Throwable cause) {
		super(message, cause);
	}

}
