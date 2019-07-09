package br.com.wps.smartnet.ssl;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;

import org.joda.time.DateTime;

import br.com.wps.smartnet.core.EnumMessageType;
import br.com.wps.smartnet.exception.UnwrapError;
import br.com.wps.smartnet.exception.WrapError;
import br.com.wps.smartnetutils.ext.Function;
import br.com.wps.smartnetutils.ext.MemCopy;

public abstract class SSLMessage implements AutoCloseable, Comparable<SSLMessage> {

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

	private SSLSession session;
	
	private DateTime createDateTime;

	private long syncCode;
	
	private int synchTimeoutInSeconds;
	
	private EnumMessageType type;

	protected ByteBuffer content;
	
	public SSLMessage() {
		
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
	
	public SSLMessage(ByteBuffer content) {
		
		this();
		
		this.content = content;
		
	}

	public SSLMessage(SSLSession session, ByteBuffer content) {
		
		this();
		
		this.session = session;
		this.content = content;
		
	}
	
	public final SSLSession getSession() {
		return session;
	}

	public final void setSession(SSLSession session) {
		this.session = session;
	}

	public long getId() {
		return id;
	}

	public void setCreateDateTime(DateTime createDateTime) {
		this.createDateTime = createDateTime;
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
	
	public abstract void unwrap() throws UnwrapError;
	
	public static <SRC extends SSLMessage, DEST extends SSLMessage> DEST copy(SRC src, Class<? extends DEST> classDest) {
		
		if (src == null || classDest == null) {
			return null;
		}

		DEST result = null;

		try {
		
			result = classDest.newInstance();
	
			if (!(src.getClass().equals(SSLMessage.class) || result.getClass().equals(SSLMessage.class))) {
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
	public SSLMessage copy() {
		
		SSLMessage result = new SSLMessage();
		
		result.setSession(session);
		
		result.setCreateDateTime(createDateTime);
		
		result.setSyncCode(nextSyncCode);;

		result.setSynchTimeoutInSeconds(synchTimeoutInSeconds);

		result.setType(type);

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
		SSLMessage other = (SSLMessage) obj;
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
	public int compareTo(SSLMessage toComparate) {

		if (this.hashCode()  < toComparate.hashCode()) {
			return -1;
		}
		
		if (this.hashCode()  > toComparate.hashCode()) {
			return 1;
		}

		return 0;
				
	}
	
	 
	
	
}
