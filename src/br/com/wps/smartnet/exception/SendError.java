package br.com.wps.smartnet.exception;

public class SendError extends Exception {

	private static final long serialVersionUID = 4425306851373021324L;

	public SendError(String message) {
		super(message);
	}
	
	public SendError(String message, Throwable cause) {
		super(message, cause);
	}

}
