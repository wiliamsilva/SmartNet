package br.com.wps.smartnet.test.sslserver;

import java.nio.ByteBuffer;

import br.com.wps.smartnet.ssl.SSLMessageEncoder;

public class EncoderTest extends SSLMessageEncoder<MessageTest> {

	@Override
	public ByteBuffer encode(MessageTest message) {
		
		ByteBuffer result = null;
		
		if (message.getContent() != null) {
			result = ByteBuffer.allocate(message.getContent().capacity() + 1);
			result.put(message.getContent().array());
			result.put((byte) 0x0A);
			result.flip();
		}
		
		return result;
	}

	@Override
	public void close() {

	}

}
