package br.com.wps.smartnet.test.client;

import java.nio.ByteBuffer;

import br.com.wps.smartnet.core.Message;
import br.com.wps.smartnet.core.Session;

public class MessageTest extends Message {

	private String text;

	public MessageTest() {
		super();
		unwrap();
	}	
	
	public MessageTest(Session session, Message message) {
		super(session, message == null? null: message.getContent());
		unwrap();
	}
	
	public MessageTest(Session session, ByteBuffer content) {
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
