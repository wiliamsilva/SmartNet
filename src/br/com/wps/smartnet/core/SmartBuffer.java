package br.com.wps.smartnet.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class SmartBuffer implements AutoCloseable {

	public static final int KB_SIZE = 1024; 
	public static final int DEFAULT_MAX_SIZE = KB_SIZE * 8; 

	private AtomicReference<ByteBuffer> refBuffer;
	private volatile int size;
	private int remaining;
	private final int bufferMaxSize;

	private Object lock = new Object();
	   
	public int getSize() {
		return this.size;
	}

	public ByteBuffer getBuffer() {
		return this.refBuffer.get();
	}
	
	
	public SmartBuffer() {
		this(SmartBuffer.DEFAULT_MAX_SIZE);
	}

	public SmartBuffer(int bufferMaxSize) {
		this.bufferMaxSize = Integer.max(KB_SIZE, bufferMaxSize);
		reset();
	}

	private void reset() {

		//this.bufferMaxSize = Integer.max(KB_SIZE, this.bufferMaxSize);
		ByteBuffer buffer = null;
		this.refBuffer = new AtomicReference<ByteBuffer>(buffer);
		this.size = 0;
		this.remaining = bufferMaxSize;
		
	}
	
	public void write(byte[] bytes) throws IOException {

		if (this.refBuffer == null || bytes == null || bytes.length == 0)
			return;
		
		ByteBuffer buffer = refBuffer.get();
		
		synchronized(lock) {

			/**
			if (this.remaining < bytes.length) {
				throw new IOException(String.format("Buffer overflow. Remaining bytes: %d, Tried to write: %d", this.remaining, bytes.length));
			}*/

			final double multiple = Math.round(((double) bytes.length / KB_SIZE)+0.5d);
			
			final int nextIncrease = ((int) multiple) * KB_SIZE;
			//nextSize = (multiple - nextSize > 0.0? nextSize + KB_SIZE: nextSize);
			
			if (buffer == null) {
				
				buffer = ByteBuffer.allocate(nextIncrease);
			}
			// Removida a limitação por tamanho máximo porque o buffer é dinâmico e volta ao tamanho mínimo
			// quando utilizado
			//else if (buffer.remaining() < bytes.length && (buffer.capacity() + nextIncrease) <= this.bufferMaxSize) {
			else if (buffer.remaining() < bytes.length) {

				final int _lastPosition = buffer.position();

				ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() + nextIncrease);

				buffer.position(0);

				newBuffer.put(buffer);
				
				newBuffer.position(_lastPosition);

				buffer = newBuffer;

			}
			// Removida a limitação por tamanho máximo porque o buffer é dinâmico e volta ao tamanho mínimo
			// quando utilizado
			/**
			else if (this.remaining < nextIncrease) {
				throw new IOException("SmartBuffer overflow");
			}*/
			

			StringBuffer sb = new StringBuffer();
			sb.append("bytes to write: ");
			sb.append(bytes.length);
			sb.append(", multiple: ");
			sb.append(multiple);
			sb.append(", nextSize: ");
			sb.append(nextIncrease);
			sb.append(", buffer position: ");
			sb.append(buffer.position());
			sb.append(", buffer capacity: ");
			sb.append(buffer.capacity());
			sb.append(", buffer limit: ");
			sb.append(buffer.limit());				
			sb.append(", buffer remaining: ");
			sb.append(buffer.remaining());				
			sb.append(", this.remaining: ");
			sb.append(this.remaining);
			sb.append(", this.size: ");
			sb.append(this.size);
			sb.append(", this.bufferMaxSize: ");
			sb.append(this.bufferMaxSize);

			try {

				buffer.put(bytes);
			}
			catch (Exception e) {

				System.out.println(sb.toString());
				e.printStackTrace();
				throw e;
			}
			catch (Throwable e) {
				System.out.println(sb.toString());
				e.printStackTrace();
				throw e;
			}

			this.size = buffer.position();
			this.remaining = bufferMaxSize - this.size;
			refBuffer.set(buffer);

		}
	
	}

	public int read(byte[] bytes) {

		int target, position, remainingAfterRead, capacity;
		byte[] completeBuffer = null;
		byte[] remainingBuffer = null;

		Arrays.fill(bytes, (byte) 0x00);
		
		synchronized(lock) {

			ByteBuffer buffer = refBuffer.get();

			if (bytes == null || bytes.length == 0 || buffer == null) {
				return 0;
			}
			
			if (this.size == 0) {
				return 0;
			}

			target = bytes.length; // tamanho para leitura
			position = buffer.position(); // posição atual do buffer
			capacity = buffer.capacity(); // total do buffer
	
			// Se o tamanho para leitura esperado for
			// maior do que o disponível para leitura,
			// então ajusta o tamanho esperado para o disponível
			if (target > this.size) { 
				target = this.size;
			}

			remainingAfterRead = position - target;
			completeBuffer = new byte[position];
			remainingBuffer = new byte[remainingAfterRead];
			
			buffer.flip();

			buffer.get(completeBuffer);

			System.arraycopy(completeBuffer, 0, bytes, 0, target);
			
			if (remainingAfterRead > 0) {
				
				System.arraycopy(completeBuffer, target, remainingBuffer, 0, remainingAfterRead);
				
				final double multiple = Math.round(((double) remainingAfterRead / KB_SIZE)+0.5d);

				final int newSize = ((int) multiple) * KB_SIZE;

				// Reduz o buffer para melhorar memória
				if (newSize < capacity) {
					buffer = ByteBuffer.allocate(newSize);
				} else {
					buffer.clear();
				}

				//buffer = ByteBuffer.allocate(newSize);
				
				buffer.put(remainingBuffer);

				this.size = buffer.position();
				this.remaining = bufferMaxSize - this.size;

			} else {

				buffer = null;
				this.size = 0;
				this.remaining = bufferMaxSize;

			}
			
			refBuffer.set(buffer);
			
		}

		return target;

	}
	
	public synchronized void clear() {

		synchronized (lock) {
			reset();
		}

	}
		
	public static void main(String args[]) {

		int readSize = 0;
		@SuppressWarnings("resource")
		SmartBuffer b = new SmartBuffer();
		byte[] buffer = new byte[6];

		try {

			b.write("teste1".getBytes());
			b.write("teste2".getBytes());
			b.write("teste3".getBytes());
			
			readSize = b.read(buffer);
			System.out.println("Size: " + readSize + " - " + new String(buffer));
			
			b.write("teste4".getBytes());

			readSize = b.read(buffer);
			System.out.println("Size: " + readSize + " - " + new String(buffer));

			readSize = b.read(buffer);
			System.out.println("Size: " + readSize + " - " + new String(buffer));

			readSize = b.read(buffer);
			System.out.println("Size: " + readSize + " - " + new String(buffer));

			System.out.println(b.getSize());

			b.write("teste5".getBytes());
			b.write("teste6".getBytes());
			b.write("teste7".getBytes());
			
			readSize = b.read(buffer);
			System.out.println("Size: " + readSize + " - " + new String(buffer));

			readSize = b.read(buffer);
			System.out.println("Size: " + readSize + " - " + new String(buffer));
			
			readSize = b.read(buffer);
			System.out.println("Size: " + readSize + " - " + new String(buffer));

			readSize = b.read(buffer);
			System.out.println("Size: " + readSize + " - " + new String(buffer));

			readSize = b.read(buffer);
			System.out.println("Size: " + readSize + " - " + new String(buffer));

			readSize = b.read(buffer);
			System.out.println("Size: " + readSize + " - " + new String(buffer));
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
	}

	public int getBufferMaxSize() {
		return bufferMaxSize;
	}

	@Override
	public void close() throws Exception {

		this.clear();
		
		if (lock != null) {
			lock = null;
		}
		
	}
	
}
