package br.com.wps.smartnet.test.client;

import java.nio.ByteBuffer;

import br.com.wps.smartnet.core.MessageEncoder;

public class EncoderTest extends MessageEncoder<MessageTest> {

	@Override
	public ByteBuffer encode(MessageTest message) {
		
		ByteBuffer result = null;
		
		if (message.getContent() != null) {
			result = ByteBuffer.allocate(message.getContent().capacity() + 1);
			result.put(message.getContent());
			result.put((byte) 0x0A);
			result.flip();
		}

		return result;
	}

	@Override
	public void close() {

	}

}
