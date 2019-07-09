package br.com.wps.smartnet.exception;

public class UnexpectedError extends Exception {

	private static final long serialVersionUID = -2702549416817807952L;

	public UnexpectedError(String message) {
		super(message);
	}
	
	public UnexpectedError(String message, Throwable cause) {
		super(message, cause);
	}

}
