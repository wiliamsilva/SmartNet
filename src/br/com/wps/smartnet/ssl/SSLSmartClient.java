package br.com.wps.smartnet.ssl;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;

import br.com.wps.smartnet.core.ClientConfiguration;
import br.com.wps.smartnet.core.Configuration;
import br.com.wps.smartnet.core.EnumSelectorConnectorStrategy;
import br.com.wps.smartnet.core.InstanceSequence;
import br.com.wps.smartnet.core.ServerConfiguration;
import br.com.wps.smartnet.exception.ClientError;
import br.com.wps.smartnet.exception.ConnectorError;
import br.com.wps.smartnet.exception.InvalidParameterError;
import br.com.wps.smartnet.exception.MultiplexerError;
import br.com.wps.smartnet.exception.NotFound;
import br.com.wps.smartnet.exception.OpenError;
import br.com.wps.smartnet.exception.SendError;
import br.com.wps.smartnet.exception.UnexpectedError;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnetutils.collection.SmartConcurrentHashMap;
import br.com.wps.smartnetutils.device.NetworkCard;
import br.com.wps.smartnetutils.ext.EnumIPAddressType;
import br.com.wps.smartnetutils.ext.ExceptionUtils;
import br.com.wps.smartnetutils.ext.SmartThread;
import br.com.wps.smartnetutils.hardware.Hardware;
import br.com.wps.smartnetutils.valueobject.IPAddress;

public class SSLSmartClient extends SmartThread implements AutoCloseable {

	public final static int DEFAULT_CLOSING_TIMEOUT = 10;    
	
	private Set<IPAddress> listeningParameters;
	private SmartConcurrentHashMap<String, SSLConnector> connectorList;
	private SmartConcurrentHashMap<String, SSLMultiplexer> multiplexerList;

	private ClientConfiguration configuration;

	private Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass;
	private Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass;	
	private SSLClientEventFactory eventFactory;

	private SSLConnectorSelector connectorSelector;

	private long instanceNumber;
	private SmartLog logger;
	
    private SSLContext context;
	
	private boolean started;

	private SSLSmartClient(String id, ClientConfiguration configuration, Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass, Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass) {

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
			this.connectorSelector = new SSLDefaultConnectorSelector(); 
		}
		else if (this.configuration.getSelectorStrategy().getValue() == EnumSelectorConnectorStrategy.BalanceConnector.getValue()) {
			this.connectorSelector = new SSLBalanceConnectorSelector(); 
		}
		else {
			this.connectorSelector = new SSLDefaultConnectorSelector(); 
		}
		
		this.context = null;
		
		started = false;
		
	}
	
	public SSLSmartClient(String id, ClientConfiguration configuration, Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass, Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass, SSLClientEventFactory eventFactory) throws InvalidParameterError {

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

		initSSLContext(configuration);
		
	}

	public SSLSmartClient(String id, ClientConfiguration configuration, Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass, Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass, SSLClientEventFactory eventFactory, String addressParameters) throws InvalidParameterError, UnexpectedError, OpenError, ClientError {

		this(id, configuration, decoderClass, encoderClass, eventFactory);

		open(addressParameters);

	}

	public void initSSLContext(Configuration configuration) throws InvalidParameterError {

		try {

			this.context = SSLContext.getInstance(ServerConfiguration.DEFAULT_PROTOCOL);

			//context.init(null,null,null);
			//SSLContext.setDefault(context);

	        context.init(SSLBasicSocket.createKeyManagers(configuration.getKeyStoreFile(), configuration.getKeyStorePassword(), configuration.getKeyPassword()), SSLBasicSocket.createTrustManagers(configuration.getTrustStoreFile(), configuration.getTrustStorePassword()), new SecureRandom());

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}	

	public long getInstanceNumber() {
		return instanceNumber;
	}

	SSLContext getContext() {
		return context;
	}

	
	SSLClientEventFactory getEventFactory() {
		return eventFactory;
	}

	Class<? extends SSLMessageDecoder<? extends SSLMessage>> getDecoderClass() {
		return decoderClass;
	}


	void setDecoderClass(Class<? extends SSLMessageDecoder<? extends SSLMessage>> decoderClass) {
		this.decoderClass = decoderClass;
	}


	Class<? extends SSLMessageEncoder<? extends SSLMessage>> getEncoderClass() {
		return encoderClass;
	}


	void setEncoderClass(Class<? extends SSLMessageEncoder<? extends SSLMessage>> encoderClass) {
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
		connectorList = new SmartConcurrentHashMap<String, SSLConnector>("connectorList");
		multiplexerList = new SmartConcurrentHashMap<String, SSLMultiplexer>("multiplexerList");

		for (IPAddress ipAddress : listeningParameters) {

			try {

				String key = null;

				SSLConnector connector = null;
				
				List<NetworkCard> listNetworkCards = ipAddress.getRelatedNetworkCards();

				SSLMultiplexer.createMultiplexer(configuration, decoderClass, encoderClass, listNetworkCards, multiplexerList, eventFactory);
				
				key = createKeyNameForConnector(ipAddress);

				connector = new SSLConnector(key, this, ipAddress, multiplexerList, eventFactory, decoderClass, encoderClass);

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

	public SSLConnector autoSelectConnector() throws NotFound {
		
		SSLConnector result = null;
		
		result = this.connectorSelector.select(this.connectorList);

		return result;
		
	}
	
	public SSLConnector getConnector(String hostaddress, int port) {

		SSLConnector result = null;
		
		String key = createKeyNameForConnector(hostaddress, port);

		result = connectorList.get(key);
		
		return result;
		
	}

	public SSLConnector getConnector(IPAddress serverAddress) {

		SSLConnector result = null;
		
		String key = createKeyNameForConnector(serverAddress);

		result = connectorList.get(key);
		
		return result;
		
	}

	public boolean sendAndConfirm(SSLMessage message) throws SendError, NotFound {
		return sendAndConfirm(null, message);
	}
	
	public boolean sendAndConfirm(IPAddress serverAddress, SSLMessage message) throws SendError, NotFound {

		SSLConnector connector = null;
		
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

	public SSLMessage sendAndReceive(SSLMessage message) throws SendError, NotFound {
		return sendAndReceive(null, message);
	}
	
	public SSLMessage sendAndReceive(IPAddress serverAddress, SSLMessage message) throws SendError, NotFound {

		SSLConnector connector = null;
		
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

	public void send(SSLMessage message) throws SendError, NotFound {
		send(null, message);
	}

	public void send(IPAddress serverAddress, SSLMessage message) throws SendError, NotFound {

		SSLConnector connector = null;
		
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

	public void send(SSLMessage message, Runnable successHandler, Runnable failureHandler) throws SendError, NotFound {
		send(null, message, successHandler, failureHandler);
	}
	
	public void send(IPAddress serverAddress, SSLMessage message, Runnable successHandler, Runnable failureHandler) throws SendError, NotFound {
	
		SSLConnector connector = null;
		
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

			SSLConnector connector = null;

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

			SSLMultiplexer multiplexer = null;

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

		//GeneralExecutor.close();
		
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
