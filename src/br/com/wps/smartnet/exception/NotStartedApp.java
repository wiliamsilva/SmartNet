package br.com.wps.smartnet.exception;

public class NotStartedApp extends Exception {

	private static final long serialVersionUID = -5662374082076720745L;

	public NotStartedApp(String message) {
		super(message);
	}
	
	public NotStartedApp(String message, Throwable cause) {
		super(message, cause);
	}

}
