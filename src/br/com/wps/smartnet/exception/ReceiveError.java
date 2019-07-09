package br.com.wps.smartnet.exception;

public class ReceiveError extends Exception {

	private static final long serialVersionUID = 4425306851373021324L;

	public ReceiveError(String message) {
		super(message);
	}
	
	public ReceiveError(String message, Throwable cause) {
		super(message, cause);
	}

}
