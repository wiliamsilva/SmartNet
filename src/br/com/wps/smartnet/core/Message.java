package br.com.wps.smartnet.core;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

import org.joda.time.DateTime;

import br.com.wps.smartnet.exception.UnwrapError;
import br.com.wps.smartnet.exception.WrapError;
import br.com.wps.smartnetutils.ext.Function;
import br.com.wps.smartnetutils.ext.MemCopy;

public abstract class Message implements AutoCloseable, Comparable<Message> {

	private static long createdInstances;
	
	private static long nextSyncCode;
	
	static {
		nextSyncCode = 0L;
		createdInstances = 0L;
	}
	
	public synchronized static long nextSyncCode() {
		
		if (nextSyncCode >= Long.MAX_VALUE) {
			nextSyncCode = 0L;
		}
		
		nextSyncCode++;
		
		return nextSyncCode;
		
	}

	private long id;

	private Session session;
	
	private DateTime createDateTime;

	private long syncCode;
	
	private int synchTimeoutInSeconds;
	
	private EnumMessageType type;

	protected ByteBuffer content;
	
	public Message() {
		
		if (createdInstances == Long.MAX_VALUE) {
			createdInstances = 0L;
		}
		createdInstances++;
		
		this.id = createdInstances;

		this.createDateTime = DateTime.now();
		this.syncCode = 0L;
		this.synchTimeoutInSeconds = 0;
		
		this.content = null;

	}
	
	public Message(ByteBuffer content) {
		
		this();
		
		this.content = content;
		
	}

	public Message(Session session, ByteBuffer content) {
		
		this();
		
		this.session = session;
		this.content = content;
		
	}
	
	public final Session getSession() {
		return session;
	}

	public final void setSession(Session session) {
		this.session = session;
	}

	public ByteBuffer getContent() {
		return content;
	}

	public String getHexaContent() {
		
		String result = Function.byteBufferToString(this.content, true);
		return result;

	}	
	
	public void setContent(ByteBuffer content) {
		if (this.content != null) {
			this.content.clear();
		}
		this.content = content;
	}
	
	public void setCreateDateTime(DateTime createDateTime) {
		this.createDateTime = createDateTime;
	}

	public DateTime getCreateDateTime() {
		return createDateTime;
	}

	public void close() {
		if (this.content != null) {
			this.content.clear();
		}
		this.content = null;
	}

	public long getSyncCode() {
		return syncCode;
	}

	public void setSyncCode(long syncCode) {
		this.syncCode = syncCode;
	}
	
	public int getSynchTimeoutInSeconds() {
		return synchTimeoutInSeconds;
	}

	public void setSynchTimeoutInSeconds(int synchTimeoutInSeconds) {
		this.synchTimeoutInSeconds = synchTimeoutInSeconds;
	}

	public EnumMessageType getType() {
		return type;
	}

	public void setType(EnumMessageType type) {
		this.type = type;
	}

	public abstract void wrap() throws WrapError;
	
	public abstract void unwrap()  throws UnwrapError;
	
	public static <SRC extends Message, DEST extends Message> DEST copy(SRC src, Class<? extends DEST> classDest) {
		
		if (src == null || classDest == null) {
			return null;
		}

		DEST result = null;

		try {
		
			result = classDest.newInstance();
	
			if (!(src.getClass().equals(Message.class) || result.getClass().equals(Message.class))) {
				MemCopy.copyObject(src, result);
			}

		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {

			result = null;

		}
		
		result.setSession(src.getSession());
		
		result.setCreateDateTime(src.getCreateDateTime());
		
		result.setSyncCode(src.getSyncCode());

		result.setSynchTimeoutInSeconds(src.getSynchTimeoutInSeconds());

		result.setType(src.getType());

		if (src.getContent() != null) {
			ByteBuffer duplicated = Function.copyByteBuffer(src.getContent());
			result.setContent(duplicated);
		}		

		return result;			

	}	

	/**
	public Message copy() {
		
		Message result = new Message();
		
		if (content != null) {
			ByteBuffer duplicated = Function.copyByteBuffer(content);
			result.setContent(duplicated);
		}
		
		return result;
		
	}*/
	
	int size() {
		
		int result = 0;
		
		if (content != null) {
			result = content.capacity();
		}
		
		return result;
		
	}
	
	@Override
	public int hashCode() {
		return (int) this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Message other = (Message) obj;
		if (createDateTime == null) {
			if (other.createDateTime != null)
				return false;
		} else if (!createDateTime.equals(other.createDateTime))
			return false;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public int compareTo(Message toComparate) {

		if (this.hashCode()  < toComparate.hashCode()) {
			return -1;
		}
		
		if (this.hashCode()  > toComparate.hashCode()) {
			return 1;
		}

		return 0;
				
	}
	
}
