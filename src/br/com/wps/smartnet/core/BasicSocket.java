package br.com.wps.smartnet.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTime;

import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnetutils.collection.SmartConcurrentHashMap;
import br.com.wps.smartnetutils.ext.GeneralExecutor;

public class BasicSocket implements AutoCloseable, Comparable<BasicSocket> {

	private final static int VERIFY_CONNECTION_INTERVAL_A = 2000; 
	private final static int VERIFY_CONNECTION_INTERVAL_B = 4000; 
	
	private static final int INTEVAL_TO_DESTROY_IN_SECONDS = 1; 
	
	private static long createdInstances;
	
	private long id; 
	
	private AtomicBoolean syncMode;
	private AtomicLong currentSyncCode;
	private DateTime dateTimeSyncMode;
	private DateTime synchTimeout;
	
	private EnumMessageType sendSyncType;

	private Message syncMessageToSend;
	
	private SocketChannel socketChannel;

	private MessageDecoder<Message> decoder;
	private MessageEncoder<Message> encoder;

	private SmartConcurrentHashMap<Long, Boolean> syncReceiveConfirmationQueue;
	private SmartConcurrentHashMap<Long, Message> syncReceiveMessageQueue;

	private SmartBuffer receiveArea;
	
	private String prefix4Log;
	private String hostname;
	private String hostaddress;
	private int port;

	private EnumConnectionType connectionType;
	private volatile EnumConnectionStatus clientStatus;	
	private EnumDisconnectionReason disconnectReason;
	
	private DateTime startDateTime;
	private DateTime stopDateTime;
	private DateTime timeToDestroy;
	private DateTime reconnectTime;
	private DateTime waitFirstMessageUntilTime;
	private DateTime firstMessageTime;
	private DateTime lastMessageTime;
	private DateTime permissionTimeToSend;

	private DateTime nextVerifyTime;
	private boolean applyA;

	private Runnable onOpenedEvent;
	private boolean onOpenedEventCalled;
	
	private Runnable onClosingEvent;
	private boolean onClosingEventHasCalled;
	
	private long numMessageReceive;
	
	private Multiplexer refMultiplexer;
	
	protected SmartLog logger;
	
	static {
		createdInstances = 0L;
	}

	public static long getCreatedInstantes() {
		return createdInstances;
	}
	
	public static String extractAddress(SocketChannel asyncSocket) {

		String result = null;
		
		if (asyncSocket != null) {
			try {
				InetSocketAddress socketAddress =  (InetSocketAddress) asyncSocket.getRemoteAddress();
				result = socketAddress.getAddress().getHostAddress();
			} catch (IOException e) {
				result = "[error]";
			}
		}

		return result;
		
	}

	public static String extractHostname(AsynchronousSocketChannel asyncSocket) {

		String result = null;
		
		if (asyncSocket != null) {
			try {
				InetSocketAddress socketAddress =  (InetSocketAddress) asyncSocket.getRemoteAddress();
				result = socketAddress.getHostName();
			} catch (IOException e) {
				result = "[error]";
			}
		}

		return result;
		
	}
	
	long getNumMessageReceive() {
		return numMessageReceive;
	}

	void setNumMessageReceive(long numMessageReceive) {
		this.numMessageReceive = numMessageReceive;
	}

	private BasicSocket() {
		this.logger = new SmartLog(this);
	}
	
	public BasicSocket(String prefix4Log, SocketChannel socketChannel) {

		this();
		
		if (createdInstances == Long.MAX_VALUE) {
			createdInstances = 0L;
		}
		createdInstances++;
		
		this.id = createdInstances;

		this.syncMode = new AtomicBoolean();
		this.syncMode.set(false);
		
		this.currentSyncCode = new AtomicLong(0L);
		
		this.dateTimeSyncMode = null;
		
		this.synchTimeout = null;
		
		this.sendSyncType = null;

		this.syncMessageToSend = null;
		
		this.socketChannel = socketChannel;
		
		this.decoder = null;
		this.encoder = null;
		
		this.syncReceiveConfirmationQueue = new SmartConcurrentHashMap<Long, Boolean>(String.format("Socket%d:syncReceiveConfirmationQueue", this.id));
		this.syncReceiveMessageQueue = new SmartConcurrentHashMap<Long, Message>(String.format("Socket%d:syncReceiveMessageQueue", this.id));

		this.receiveArea = new SmartBuffer(Multiplexer.DEFAULT_RECEIVE_BUFFER_SIZE);
		
		this.prefix4Log = prefix4Log;
		
		this.connectionType = EnumConnectionType.RemoteSocket;
		this.clientStatus = EnumConnectionStatus.Stopped;
		this.disconnectReason = null;
		
		this.startDateTime = DateTime.now();
		this.stopDateTime = null;
		this.timeToDestroy = null;
		this.reconnectTime = null;
		this.waitFirstMessageUntilTime = null;
		this.firstMessageTime = null;
		this.lastMessageTime = null;
		this.permissionTimeToSend = null;

		this.nextVerifyTime = null;
		this.applyA = true;
		
		if (socketChannel != null) {
		
			try {
				InetSocketAddress socketAddress =  (InetSocketAddress) socketChannel.getRemoteAddress();
				this.hostname = socketAddress.getHostName();
				this.hostaddress = socketAddress.getAddress().getHostAddress();
				this.port = socketAddress.getPort();
			} catch (IOException e) {
				this.hostname = "[error]";
				this.hostaddress = "[error]";
				this.port = 0;
			}

		} else {

			this.hostname = "[unknown]";
			this.hostaddress = "[unknown]";
			this.port = 0;

		}
		
		onOpenedEvent = null;
		onOpenedEventCalled = false;
		
		onClosingEvent = null;
		onClosingEventHasCalled = false;
		
		numMessageReceive = 0;
		
	}

	public BasicSocket(SocketChannel socketChannel) {
		this(null, socketChannel);
	}
	
	public BasicSocket(SocketChannel socketChannel, Multiplexer refMultiplexer) {
		this();
		this.refMultiplexer = refMultiplexer;
	}

	Multiplexer getRefMultiplexer() {
		return refMultiplexer;
	}

	void setRefMultiplexer(Multiplexer refMultiplexer) {
		this.refMultiplexer = refMultiplexer;
	}

	SmartBuffer getReceiveArea() {
		return receiveArea;
	}

	void setReceiveArea(SmartBuffer receiveArea) {
		this.receiveArea = receiveArea;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public SocketChannel getSocketChannel() {
		return socketChannel;
	}

	public void setAsyncSocket(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

	public MessageDecoder<Message> getDecoder() {
		return decoder;
	}

	public MessageEncoder<Message> getEncoder() {
		return encoder;
	}

	public String getPrefix4Log() {
		return prefix4Log;
	}

	public void setPrefix4Log(String prefix4Log) {
		this.prefix4Log = prefix4Log;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getHostaddress() {
		return hostaddress;
	}

	public void setHostaddress(String hostaddress) {
		this.hostaddress = hostaddress;
	}
	
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public DateTime getStartDateTime() {
		return startDateTime;
	}

	void setStartDateTime(DateTime startDateTime) {
		this.startDateTime = startDateTime;
	}

	public DateTime getStopDateTime() {
		return stopDateTime;
	}

	void setStopDateTime(DateTime stopDateTime) {
		this.stopDateTime = stopDateTime;
	}

	public EnumConnectionType getConnectionType() {
		return connectionType;
	}

	void setConnectionType(EnumConnectionType connectionType) {
		this.connectionType = connectionType;
	}

	public EnumConnectionStatus getClientStatus() {
		return clientStatus;
	}

	void updateNextVerifyTime() {

		if (this.applyA) {
			this.nextVerifyTime = DateTime.now().plusMillis(VERIFY_CONNECTION_INTERVAL_A);
		} else {
			this.nextVerifyTime = DateTime.now().plusMillis(VERIFY_CONNECTION_INTERVAL_B);
		}

		this.applyA = !this.applyA;

	}
	
	synchronized DateTime getNextVerifyTime() {

		if (nextVerifyTime == null) {
			updateNextVerifyTime();
		}
		
		return nextVerifyTime;
	}

	public boolean isActive() {
		return  (this.clientStatus.getValue() == EnumConnectionStatus.StartedFull.getValue());
	}
	
	public void prepareToDisconnect(EnumDisconnectionReason reason) {
		
		if (this.stopDateTime == null) {
			this.clientStatus = EnumConnectionStatus.ReadyToRemove;
			this.stopDateTime = DateTime.now();
			this.timeToDestroy = this.stopDateTime.plusSeconds(INTEVAL_TO_DESTROY_IN_SECONDS);
			this.disconnectReason = reason;
		}

	}

	public void close() {

		if (!onClosingEventHasCalled && this.onClosingEvent != null) {
			onClosingEventHasCalled = true;
			GeneralExecutor.executeAndWait(this.onClosingEvent, GeneralExecutor.TEN_SECONDS);
		}
		
		if (this.syncReceiveConfirmationQueue != null && syncReceiveConfirmationQueue.size() > 0) {
			this.syncReceiveConfirmationQueue.clear();
			this.syncReceiveConfirmationQueue = null;
		}

		if (this.syncReceiveMessageQueue != null && syncReceiveMessageQueue.size() > 0) {
			this.syncReceiveMessageQueue.clear();
			this.syncReceiveMessageQueue = null;
		}
		
		try {
			if (socketChannel != null && socketChannel.isOpen()) {

				socketChannel.close();

			}
			socketChannel = null;
		} catch (IOException e) {
			socketChannel = null;
		} catch (Exception e) {
			socketChannel = null;
		} catch (Throwable e) {
			socketChannel = null;
		}

		if (this.refMultiplexer != null) {
			Multiplexer.minusSession();
		}
		
		if (decoder != null) {
			decoder.close();
			decoder = null;
		}

		if (encoder != null) {
			encoder.close();
			encoder = null;
		}
		
		if (this.stopDateTime == null) {
			this.stopDateTime = DateTime.now();
		}
		
	}

	@Override
	public int compareTo(BasicSocket toComparate) {

		if (this.getId()  < toComparate.getId()) {
			return -1;
		}
		
		if (this.getId()  > toComparate.getId()) {
			return 1;
		}

		return 0;

	}

	public EnumDisconnectionReason getDisconnectReason() {
		return disconnectReason;
	};

	// M�todos acess�veis somente para classes do mesmo pacote

	void setDecoder(MessageDecoder<Message> decoder) {
		this.decoder = decoder;
	}

	void setEncoder(MessageEncoder<Message> encoder) {
		this.encoder = encoder;
	}

	protected synchronized void setClientStatus(EnumConnectionStatus clientStatus) {

		if (clientStatus.getValue() == EnumConnectionStatus.StartedFull.getValue() && !onOpenedEventCalled && onOpenedEvent != null && socketChannel != null && socketChannel.isConnected()) {
			GeneralExecutor.execute(onOpenedEvent);
			onOpenedEventCalled = true;
		}

		this.clientStatus = clientStatus;

	}

	public boolean isSyncModeOn() {
		
		return syncMode.get();
	}

	protected void turnSyncModeOn(long currentSyncCode, EnumMessageType sendSyncType, int timeoutSeconds) {
		if (!isSyncModeOn()) {
			this.syncMode.set(true);
			this.dateTimeSyncMode = DateTime.now();
			this.synchTimeout = dateTimeSyncMode.plusSeconds(timeoutSeconds);
			this.currentSyncCode.set(currentSyncCode);
			this.sendSyncType = sendSyncType;
		}
	}

	protected void turnSyncModeOff() {
		this.syncMode.set(false);
		dateTimeSyncMode = null;
		this.currentSyncCode.set(0L);
		this.sendSyncType = null;
		this.setSyncMessageToSend(null);
	}
	
	DateTime getDateTimeSyncMode() {
		return dateTimeSyncMode;
	}

	long getCurrentSyncCode() {
		return currentSyncCode.get();
	}

	DateTime getSynchTimeout() {
		return synchTimeout;
	}

	Message getSyncMessageToSend() {
		return syncMessageToSend;
	}

	protected void setSyncMessageToSend(Message syncMessageToSend) {
		
		if (this.syncMessageToSend != null && this.syncMessageToSend.getContent() != null) {
			this.syncMessageToSend.getContent().clear();
			this.syncMessageToSend.setContent(null);
		}
		this.syncMessageToSend = syncMessageToSend;

	}

	DateTime getTimeToDestroy() {
		return timeToDestroy;
	}

	void setTimeToDestroy(DateTime timeToDestroy) {
		this.timeToDestroy = timeToDestroy;
	}

	EnumMessageType getSendSyncType() {
		return sendSyncType;
	}

	DateTime getReconnectTime() {
		return reconnectTime;
	}

	void setReconnectTime(DateTime reconnectTime) {
		this.reconnectTime = reconnectTime;
	}

	DateTime getFirstMessageTime() {
		return firstMessageTime;
	}

	synchronized void setFirstMessageTime(DateTime firstMessageTime) {
		this.firstMessageTime = firstMessageTime;
	}

	DateTime getWaitFirstMessageUntilTime() {
		return waitFirstMessageUntilTime;
	}

	void setWaitFirstMessageUntilTime(DateTime waitFirstMessageUntilTime) {
		this.waitFirstMessageUntilTime = waitFirstMessageUntilTime;
	}

	DateTime getLastMessageTime() {
		return lastMessageTime;
	}

	synchronized void setLastMessageTime(DateTime lastMessageTime) {
		this.lastMessageTime = lastMessageTime;
	}

	public DateTime getPermissionTimeToSend() {
		return permissionTimeToSend;
	}

	public void setPermissionTimeToSend(int delayInMillis) {
		if (delayInMillis > 0) {
			this.permissionTimeToSend = DateTime.now().plusMillis(delayInMillis);
		} else { 
			this.permissionTimeToSend = null;
		}
			
	}

	SmartConcurrentHashMap<Long, Boolean> getSyncReceiveConfirmationQueue() {
		return syncReceiveConfirmationQueue;
	}

	void setSyncReceiveConfirmationQueue(SmartConcurrentHashMap<Long, Boolean> syncReceiveConfirmationQueue) {
		this.syncReceiveConfirmationQueue = syncReceiveConfirmationQueue;
	}

	SmartConcurrentHashMap<Long, Message> getSyncReceiveMessageQueue() {
		return syncReceiveMessageQueue;
	}

	void setSyncReceiveMessageQueue(SmartConcurrentHashMap<Long, Message> syncReceiveMessageQueue) {
		this.syncReceiveMessageQueue = syncReceiveMessageQueue;
	}
	
	public synchronized void addOnOpenedEvent(Runnable event) {

		this.onOpenedEvent = event;

	}	

	public synchronized void addOnClosingEvent(Runnable event) {

		this.onClosingEvent = event;

	}

	
}
