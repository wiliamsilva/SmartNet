package br.com.wps.smartnet.exception;

public class AccepterError extends Exception {

	private static final long serialVersionUID = 4425306851373021324L;

	public AccepterError(String message) {
		super(message);
	}
	
	public AccepterError(String message, Throwable cause) {
		super(message, cause);
	}

}
