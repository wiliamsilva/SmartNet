package br.com.wps.smartnet.test.server;

import java.nio.ByteBuffer;

import br.com.wps.smartnet.core.Message;

public class MessageTest extends Message {

	private String text;
	
	public MessageTest() {
		super();
	}
	
	public MessageTest(ByteBuffer content) {
		super(content);
	}
	
	public ByteBuffer getContent() {
		return content;
	}

	@Override
	public void close() {

	}

	@Override
	public void wrap() {

		String message = null; 
		
		if (text == null) {
			message = "";
		} else {
			message = text;
		}

		super.setContent(ByteBuffer.wrap(message.getBytes()));
		
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
