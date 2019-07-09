package br.com.wps.smartnet.exception;

public class EncoderError extends Exception {

	private static final long serialVersionUID = 4425306851373021324L;

	public EncoderError(String message) {
		super(message);
	}
	
	public EncoderError(String message, Throwable cause) {
		super(message, cause);
	}

}
