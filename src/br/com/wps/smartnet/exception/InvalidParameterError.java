package br.com.wps.smartnet.exception;

public class InvalidParameterError extends Exception {

	private static final long serialVersionUID = 305599048432153549L;

	public InvalidParameterError(String message) {
		super(message);
	}
	
	public InvalidParameterError(String message, Throwable cause) {
		super(message, cause);
	}

}
