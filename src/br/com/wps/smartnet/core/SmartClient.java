package br.com.wps.smartnet.core;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import br.com.wps.smartnet.exception.ConnectorError;
import br.com.wps.smartnet.exception.OpenError;
import br.com.wps.smartnet.exception.SendError;
import br.com.wps.smartnet.exception.InvalidParameterError;
import br.com.wps.smartnet.exception.MultiplexerError;
import br.com.wps.smartnet.exception.NotFound;
import br.com.wps.smartnet.exception.ClientError;
import br.com.wps.smartnet.exception.UnexpectedError;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnetutils.ext.ExceptionUtils;
import br.com.wps.smartnetutils.ext.GeneralExecutor;
import br.com.wps.smartnetutils.ext.SmartThread;
import br.com.wps.smartnetutils.collection.SmartConcurrentHashMap;
import br.com.wps.smartnetutils.device.NetworkCard;
import br.com.wps.smartnetutils.ext.EnumIPAddressType;
import br.com.wps.smartnetutils.hardware.Hardware;
import br.com.wps.smartnetutils.valueobject.IPAddress;

public class SmartClient extends SmartThread implements AutoCloseable {

	public final static int DEFAULT_CLOSING_TIMEOUT = 10;    
	
	private Set<IPAddress> listeningParameters;
	private SmartConcurrentHashMap<String, Connector> connectorList;
	private SmartConcurrentHashMap<String, Multiplexer> multiplexerList;

	private ClientConfiguration configuration;

	private Class<? extends MessageDecoder<? extends Message>> decoderClass;
	private Class<? extends MessageEncoder<? extends Message>> encoderClass;	
	private ClientEventFactory eventFactory;

	private ConnectorSelector connectorSelector;

	private long instanceNumber;
	private SmartLog logger;
	
	private boolean started;

	private SmartClient(String id, ClientConfiguration configuration, Class<? extends MessageDecoder<? extends Message>> decoderClass, Class<? extends MessageEncoder<? extends Message>> encoderClass) {

		super(id);
		
		instanceNumber = InstanceSequence.nextValue();
		
		logger = new SmartLog(this);
		
		this.listeningParameters = null;
		this.connectorList = null;
		this.multiplexerList = null;

		this.configuration = configuration;
		
		this.decoderClass = decoderClass;
		this.encoderClass = encoderClass;		
		this.eventFactory = null;

		// Determina a estratégia de seleção de conexão conforme configuração
		if (this.configuration == null || this.configuration.getSelectorStrategy() == null || this.configuration.getSelectorStrategy().getValue() == EnumSelectorConnectorStrategy.Default.getValue()) {
			this.connectorSelector = new DefaultConnectorSelector(); 
		}
		else if (this.configuration.getSelectorStrategy().getValue() == EnumSelectorConnectorStrategy.BalanceConnector.getValue()) {
			this.connectorSelector = new BalanceConnectorSelector(); 
		}
		else {
			this.connectorSelector = new DefaultConnectorSelector(); 
		}
		
		started = false;
		
	}
	
	public SmartClient(String id, ClientConfiguration configuration, Class<? extends MessageDecoder<? extends Message>> decoderClass, Class<? extends MessageEncoder<? extends Message>> encoderClass, ClientEventFactory eventFactory) throws InvalidParameterError {

		this(id, configuration, decoderClass, encoderClass);

		if (configuration == null) {
			logger.error("Client Configuration class not set on constructor");
			throw new InvalidParameterError("Client Configuration class not set on constructor.");
		}

		if (decoderClass == null) {
			logger.error("Decoder class not set on constructor");
			throw new InvalidParameterError("Decoder class not set on constructor.");
		}

		if (encoderClass == null) {
			logger.error("Encoder class not set on constructor");
			throw new InvalidParameterError("Encoder class not set on constructor.");
		}

		if (eventFactory == null) {
			logger.error("Event factory class not set on constructor");
			throw new InvalidParameterError("Event factory class not set on constructor.");
		}
		
		this.listeningParameters = null;
		this.connectorList = null;
		this.multiplexerList = null;

		this.eventFactory = eventFactory;

	}

	public SmartClient(String id, ClientConfiguration configuration, Class<? extends MessageDecoder<? extends Message>> decoderClass, Class<? extends MessageEncoder<? extends Message>> encoderClass, ClientEventFactory eventFactory, String addressParameters) throws InvalidParameterError, UnexpectedError, OpenError, ClientError {

		this(id, configuration, decoderClass, encoderClass, eventFactory);

		open(addressParameters);

	}


	public long getInstanceNumber() {
		return instanceNumber;
	}

	Class<? extends MessageDecoder<? extends Message>> getDecoderClass() {
		return decoderClass;
	}


	void setDecoderClass(Class<? extends MessageDecoder<? extends Message>> decoderClass) {
		this.decoderClass = decoderClass;
	}


	Class<? extends MessageEncoder<? extends Message>> getEncoderClass() {
		return encoderClass;
	}


	void setEncoderClass(Class<? extends MessageEncoder<? extends Message>> encoderClass) {
		this.encoderClass = encoderClass;
	}
	
	ClientConfiguration getConfiguration() {
		return configuration;
	}

	static String createKeyNameForConnector(String hostaddress, int port) {

		String result = null;
		
		result = String.format("%s:%d", hostaddress, port);
		
		return result;
		
	}
	
	static String createKeyNameForConnector(IPAddress ipAddress) {

		String result = null;
		
		result = createKeyNameForConnector(ipAddress.getHostaddress(),  ipAddress.getPort());
		
		return result;
		
	}

	public void open(String addressParameters) throws InvalidParameterError, UnexpectedError, OpenError, ClientError {

		if (started) {
			logger.error("This client already started");
			throw new OpenError("This client already started.");
		}

		logger.trace("Opening");

		this.eventFactory._starting();
		
		/**************************************************
		 * DECODIFICA ENDEREï¿½OS
		 **************************************************/
		int numberOfInvalidAddresses = 0;

		if (addressParameters == null || addressParameters.trim().length() == 0) {
			String errorMessage = "Null or empty addresses parameter";
			logger.error("Null or empty addresses parameter");
			eventFactory._error(new IOException(errorMessage));
			throw new InvalidParameterError("Null or empty addresses parameter.");
		}

		listeningParameters = Hardware.ipAddressStringToObject(addressParameters, Boolean.TRUE);

		if (addressParameters == null || addressParameters.trim().length() == 0) {
			String errorMessage = "Invalid addresses parameter";
			logger.error(errorMessage);
			eventFactory._error(new IOException(errorMessage));
			throw new InvalidParameterError(errorMessage);
		}

		StringBuffer out = new StringBuffer();

		for (IPAddress ipAddress : listeningParameters) {

			if (ipAddress.getType().getValue() == EnumIPAddressType.All.getValue()) {
				ipAddress.setValid(false);
				ipAddress.setError("Este tipo de endereço não é permitido no modo client.");
			}			
			
			if (!ipAddress.isValid()) {
				numberOfInvalidAddresses++;
				out.append(ipAddress.toString());
			}

			try {
				Thread.sleep(SmartThread.FAST_SLEEP_VALUE);
			} catch (InterruptedException e) {
				String errorMessage = "Thread interrupted";
				logger.error(errorMessage);
				eventFactory._error(new IOException(errorMessage));
				throw new UnexpectedError(String.format(errorMessage));
			}

		}

		if (numberOfInvalidAddresses > 0) {
			String errorMessage = String.format("Addresses parameter contains invalid address.\n%s", out.toString());
			logger.error(errorMessage);
			eventFactory._error(new IOException(errorMessage));
			throw new InvalidParameterError(errorMessage);
		}

		/**************************************************
		 * MONTA CLIENT SOCKET
		 **************************************************/
		connectorList = new SmartConcurrentHashMap<String, Connector>("connectorList");
		multiplexerList = new SmartConcurrentHashMap<String, Multiplexer>("multiplexerList");

		for (IPAddress ipAddress : listeningParameters) {

			try {

				String key = null;

				Connector connector = null;
				
				List<NetworkCard> listNetworkCards = ipAddress.getRelatedNetworkCards();

				Multiplexer.createMultiplexer(configuration, decoderClass, encoderClass, listNetworkCards, multiplexerList, eventFactory);
				
				key = createKeyNameForConnector(ipAddress);

				connector = new Connector(key, this, ipAddress, multiplexerList, eventFactory, decoderClass, encoderClass);

				logger.trace("Created a TCP/IP address connector to %s:%d port", ipAddress.getHostaddress(), ipAddress.getPort());

				connector.start();
				
				connectorList.put(key, connector);
				

			} catch (IOException e) {
				destroyObjects();
				String errorMessage = String.format("ClientSocketChannel creation failed: %s. Error: %s", ipAddress.toString(), ExceptionUtils.rootCauseMessage(e));
				logger.error(errorMessage);
				eventFactory._error(new IOException(errorMessage));
				throw new ClientError(errorMessage);
			} catch (ConnectorError e) {
				destroyObjects();
				String errorMessage = String.format("ClientSocketChannel creation failed: %s. Error: %s", ipAddress.toString(), ExceptionUtils.rootCauseMessage(e));
				logger.error(errorMessage);
				eventFactory._error(new IOException(errorMessage));
				throw new ClientError(errorMessage);
			} catch (MultiplexerError e) {
				destroyObjects();
				String errorMessage = String.format("ClientSocketChannel creation failed: %s. Error: %s", ipAddress.toString(), ExceptionUtils.rootCauseMessage(e));
				logger.error(errorMessage);
				eventFactory._error(new IOException(errorMessage));
				throw new ClientError(errorMessage);
			}

			try {
				Thread.sleep(SmartThread.FAST_SLEEP_VALUE);
			} catch (InterruptedException e) {
				destroyObjects();
				String errorMessage = String.format("ClientSocketChannel creation failed: %s. Error: %s", ipAddress.toString(), ExceptionUtils.rootCauseMessage(e));
				logger.error(errorMessage);
				eventFactory._error(new IOException(errorMessage));
				throw new ClientError(errorMessage);
			}

		}

		/**************************************************
		 * Monta Client Sockets
		 **************************************************/
		this.start();
		
		this.eventFactory._started();

		this.started = true;

	}

	public Connector autoSelectConnector() throws NotFound {
		
		Connector result = null;
		
		result = this.connectorSelector.select(this.connectorList);

		return result;
		
	}
	
	public Connector getConnector(String hostaddress, int port) {

		Connector result = null;
		
		String key = createKeyNameForConnector(hostaddress, port);

		result = connectorList.get(key);
		
		return result;
		
	}

	public Connector getConnector(IPAddress serverAddress) {

		Connector result = null;
		
		String key = createKeyNameForConnector(serverAddress);

		result = connectorList.get(key);
		
		return result;
		
	}

	public boolean sendAndConfirm(Message message) throws SendError, NotFound {
		return sendAndConfirm(null, message);
	}
	
	public boolean sendAndConfirm(IPAddress serverAddress, Message message) throws SendError, NotFound {

		Connector connector = null;
		
		if (serverAddress != null) {
			
			connector = getConnector(serverAddress);
			
			if (connector == null) {
				throw new NotFound(String.format("Connect not found for address %s", createKeyNameForConnector(serverAddress)));
			}
			
		} else {

			connector = autoSelectConnector();

		}
		
		return connector.sendAndConfirm(message);

	}

	public Message sendAndReceive(Message message) throws SendError, NotFound {
		return sendAndReceive(null, message);
	}
	
	public Message sendAndReceive(IPAddress serverAddress, Message message) throws SendError, NotFound {

		Connector connector = null;
		
		if (serverAddress != null) {
			
			connector = getConnector(serverAddress);
			
			if (connector == null) {
				throw new NotFound(String.format("Connect not found for address %s", createKeyNameForConnector(serverAddress)));
			}
			
		} else {

			connector = autoSelectConnector();

		}

		return connector.sendAndReceive(message);

	}

	public void send(Message message) throws SendError, NotFound {
		send(null, message);
	}

	public void send(IPAddress serverAddress, Message message) throws SendError, NotFound {

		Connector connector = null;
		
		if (serverAddress != null) {
			
			connector = getConnector(serverAddress);
			
			if (connector == null) {
				throw new NotFound(String.format("Connect not found for address %s", createKeyNameForConnector(serverAddress)));
			}
			
		} else {

			connector = autoSelectConnector();

		}

		connector.send(message);
		
	}

	public void send(Message message, Runnable successHandler, Runnable failureHandler) throws SendError, NotFound {
		send(null, message, successHandler, failureHandler);
	}
	
	public void send(IPAddress serverAddress, Message message, Runnable successHandler, Runnable failureHandler) throws SendError, NotFound {
	
		Connector connector = null;
		
		if (serverAddress != null) {
			
			connector = getConnector(serverAddress);
			
			if (connector == null) {
				throw new NotFound(String.format("Connect not found for address %s", createKeyNameForConnector(serverAddress)));
			}
			
		} else {

			connector = autoSelectConnector();

		}

		connector.send(message, successHandler, failureHandler);		

	}
	
	private void destroyObjects() {

		if (connectorList != null) {

			Connector connector = null;

			Set<String> connectorKeys = connectorList.keySet();
			
			for (String a: connectorKeys) {
				
				connector = connectorList.get(a);
				
				if (connector == null) {
					continue;
				}
				
				try {
					connector.close();
					connector.join(DEFAULT_CLOSING_TIMEOUT * 1000L);
				} catch (Exception e) {
					logger.error("Close error: %s", ExceptionUtils.rootCauseMessage(e));
				} finally {
					connector = null;
				}
				
			}

			connectorList.clear();

			connectorList = null;

		}

		if (multiplexerList != null) {

			Multiplexer multiplexer = null;

			Set<String> multiplexerKeys = multiplexerList.keySet();
			
			for (String mp: multiplexerKeys) {
				
				multiplexer = multiplexerList.get(mp);
				
				if (multiplexer == null) {
					continue;
				}
				
				try {
					multiplexer.close();
					multiplexer.join(DEFAULT_CLOSING_TIMEOUT * 1000L);
				} catch (Exception e) {
					logger.error("Close error: %s", ExceptionUtils.rootCauseMessage(e));
				} finally {
					multiplexer = null;
				}
				
			}

			multiplexerList.clear();

			multiplexerList = null;

		}

		if (listeningParameters != null) {

			listeningParameters.clear();

			listeningParameters = null;

		}

		GeneralExecutor.close();

	}

	@Override
	public void close() throws Exception {

		this.stopWork();

		destroyObjects();
		
	}

	@Override
	public void doWorkAndRepeat() throws IOException, Throwable {


	}

	@Override
	public void startEvent() {

		logger.trace("Started client %d - %s", super.getId(), super.getName());
		
	}

	@Override
	public void pauseEvent() {

		logger.trace("Paused client %d - %s", super.getId(), super.getName());
		
	}

	@Override
	public void stopEvent() {

		logger.trace("Stopped client %d - %s", super.getId(), super.getName());
		
	}

	@Override
	public void resumeEvent() {

		logger.trace("Resumed client %d - %s", super.getId(), super.getName());
		
	}

}
