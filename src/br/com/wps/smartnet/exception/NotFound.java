package br.com.wps.smartnet.exception;

public class NotFound extends Exception {

	private static final long serialVersionUID = -8458088903792440903L;

	public NotFound(String message) {
		super(message);
	}
	
	public NotFound(String message, Throwable cause) {
		super(message, cause);
	}

}
