package br.com.wps.smartnet.exception;

public class SelectorError extends Exception {

	private static final long serialVersionUID = 4425306851373021324L;

	public SelectorError(String message) {
		super(message);
	}
	
	public SelectorError(String message, Throwable cause) {
		super(message, cause);
	}

}
