package br.com.wps.smartnet.test.client;

import java.io.IOException;
import java.nio.ByteBuffer;

import br.com.wps.smartnet.core.MessageDecoder;
import br.com.wps.smartnet.core.SmartBuffer;
import br.com.wps.smartnet.exception.DecoderError;
import br.com.wps.smartnetutils.ext.SmartThread;

public class DecoderTest extends MessageDecoder<MessageTest> {

	private SmartBuffer remainingBuffer; 
	
	public DecoderTest() {
		remainingBuffer = new SmartBuffer();
	}
	
	
	@Override
	public void close() {
		
		if (remainingBuffer != null) {
			remainingBuffer.clear();
			remainingBuffer = null;
		}
		
	}

	@Override
	public MessageTest decode(SmartBuffer fireBuffer) throws DecoderError {

		MessageTest result = null;

		if (fireBuffer == null || fireBuffer.getSize() <= 0) {
			return result;
		}

		byte[] currentByte = new byte[1];

		while (fireBuffer.getSize() > 0) {
			
			fireBuffer.read(currentByte);

			if (currentByte[0] == (byte) 0x0A) {

				if (remainingBuffer.getSize() > 0) {
				
					byte[] content = new byte[remainingBuffer.getSize()];
					
					remainingBuffer.read(content);
	
					ByteBuffer buffer = ByteBuffer.wrap(content);
	
					result = new MessageTest(super.getRefSession(), buffer);
					
					break;
					
				}
				
			} else {
			
				try {
					remainingBuffer.write(currentByte);
				} catch (IOException e) {
					e.printStackTrace();
				}
			
			}

			try {
				Thread.sleep(SmartThread.FAST_SLEEP_VALUE);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
		
		return result;
	
	}

}
