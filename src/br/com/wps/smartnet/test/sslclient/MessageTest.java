package br.com.wps.smartnet.test.sslclient;

import java.nio.ByteBuffer;

import br.com.wps.smartnet.ssl.SSLMessage;
import br.com.wps.smartnet.ssl.SSLSession;

public class MessageTest extends SSLMessage {

	private String text;

	public MessageTest() {
		super();
		unwrap();
	}	
	
	public MessageTest(SSLSession session, SSLMessage message) {
		super(session, message == null? null: message.getContent());
		unwrap();
	}
	
	public MessageTest(SSLSession session, ByteBuffer content) {
		super(session, content);
		unwrap();
	}	

	@Override
	public void wrap() {

		if (text != null) {
			super.setContent(ByteBuffer.wrap(text.getBytes()));
		} else {
			super.setContent(null);
		}
		
	}

	@Override
	public void unwrap() {

		if (super.getContent() == null)
			return;

		//System.err.println("Message unwrap size " + super.getContent().capacity());
		//System.err.println("Message unwrap position" + super.getContent().capacity());
		
		byte[] auxBuffer = new byte[super.getContent().capacity()];
		
		super.getContent().position(0);
		
		super.getContent().get(auxBuffer, 0, super.getContent().capacity());
		
		this.text = new String(auxBuffer);

	}
	
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}


}
