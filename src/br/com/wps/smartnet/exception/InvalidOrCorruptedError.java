package br.com.wps.smartnet.exception;

public class InvalidOrCorruptedError extends Exception {

	private static final long serialVersionUID = -3791890079826594692L;

	public InvalidOrCorruptedError(String message) {
		super(message);
	}
	
	public InvalidOrCorruptedError(String message, Throwable cause) {
		super(message, cause);
	}

}
