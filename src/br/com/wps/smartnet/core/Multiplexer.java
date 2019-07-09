package br.com.wps.smartnet.core;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;

import br.com.wps.smartnet.exception.DecoderError;
import br.com.wps.smartnet.exception.EncoderError;
import br.com.wps.smartnet.exception.MultiplexerError;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnetutils.collection.SmartConcurrentHashMap;
import br.com.wps.smartnetutils.collection.SmartConcurrentLinkedQueue;
import br.com.wps.smartnetutils.collection.SmartTimeObject;
import br.com.wps.smartnetutils.device.NetworkAddress;
import br.com.wps.smartnetutils.device.NetworkCard;
import br.com.wps.smartnetutils.ext.ExceptionUtils;
import br.com.wps.smartnetutils.ext.Function;
import br.com.wps.smartnetutils.ext.ReflectionUtils;
import br.com.wps.smartnetutils.ext.SmartThread;


public class Multiplexer extends SmartThread implements AutoCloseable {

	public static final int EXPIRATION_OFF = 0;
	
	public static final int DEFAULT_RECEIVE_BUFFER_SIZE = 4096; 
	
	public static final String FOR_UNKNOWN_SOCKET = "for:unknown:socket";

	public static Object object;
	
	public static long numberOfSessions;

	static {
		object = new Object();
		numberOfSessions = 0L;
	}
	
	public static void plusSession() {
		
		synchronized (object) {
			numberOfSessions++;
		}
		
	}
	
	public static void minusSession() {
		
		synchronized (object) {
			numberOfSessions--;
		}
		
	}
	
	public static long getNumberOfSession() {

		synchronized (object) {
			return numberOfSessions;
		}
		
	}
	
	private NetworkCard networkCard;
	
	private Selector selector;

	private Configuration refConfiguration;
	
	private Class<? extends MessageDecoder<? extends Message>> decoderClass;
	private Class<? extends MessageEncoder<? extends Message>> encoderClass;
	
	//private MessageDecoder<? extends Message> decoder;
	
	private EventFactory refEventFactory;
	private SmartConcurrentHashMap<SocketChannel, Session> sessionList;
	private SmartConcurrentLinkedQueue<Message> outputMessageQueue;
	private SmartConcurrentLinkedQueue<ChangeRequest> pendingChanges;
	private SmartConcurrentHashMap<SocketChannel, ArrayList<ByteBuffer>> pendingData;

	public Class<? extends Message> decoderMessageType;
	public Class<? extends Message> encoderMessageType;
	
	private ByteBuffer readBuffer;
	
	private long instanceNumber;
	private SmartLog logger;
	
	private boolean started;
	
	public Multiplexer(String id, Configuration refConfiguration, Class<? extends MessageDecoder<? extends Message>> decoderClass, Class<? extends MessageEncoder<? extends Message>> encoderClass, NetworkCard networkCard, EventFactory refEventFactory) throws MultiplexerError {

		super(id);

		instanceNumber = InstanceSequence.nextValue();

		logger = new SmartLog(this);		

		this.refConfiguration = refConfiguration;
		
		this.decoderClass = decoderClass;
		
		this.encoderClass = encoderClass;

		this.networkCard = networkCard;

		this.refEventFactory = refEventFactory;

		this.sessionList = new SmartConcurrentHashMap<>(String.format("SessionList:%d", instanceNumber), EXPIRATION_OFF);

		this.outputMessageQueue = new SmartConcurrentLinkedQueue<Message>(String.format("OutputMessageQueue:%d", instanceNumber));

		this.pendingChanges = new SmartConcurrentLinkedQueue<ChangeRequest>(String.format("PendingChanges:%d", instanceNumber));

		this.pendingData = new SmartConcurrentHashMap<SocketChannel, ArrayList<ByteBuffer>>(String.format("PendingData:%d", instanceNumber)); ;

		/**
		try {
			this.decoder = ReflectionUtils.<MessageDecoder<Message>>newInstance(decoderClass);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new MultiplexerError(String.format("Error while creating decoder instance. %s",  ExceptionUtils.rootCauseMessage(e)));
		}*/

		this.decoderMessageType = ReflectionUtils.<Message>getTypeParameterClass(this.decoderClass, 0);

		this.encoderMessageType = ReflectionUtils.<Message>getTypeParameterClass(this.encoderClass, 0);
		
		this.readBuffer = ByteBuffer.allocate(DEFAULT_RECEIVE_BUFFER_SIZE);

		try {
			this.selector = SelectorProvider.provider().openSelector();
		} catch (Exception e) {
			logger.error("Error while opening selector: %s", ExceptionUtils.rootCauseMessage(e));
			destroyObjects();
			throw new MultiplexerError(String.format("Error while opening selector: %s.", ExceptionUtils.rootCauseMessage(e)));
		} catch (Throwable e) {
			logger.fatal("Error while opening selector: %s", ExceptionUtils.rootCauseMessage(e));
			destroyObjects();
			throw new MultiplexerError(String.format("Error while opening selector: %s.", ExceptionUtils.rootCauseMessage(e)));
		}
		
		
		started = true;

	}

	public Multiplexer(String id, Configuration refConfiguration, Class<? extends MessageDecoder<? extends Message>> decoderClass, Class<? extends MessageEncoder<? extends Message>> encoderClass, EventFactory refEventFactory) throws MultiplexerError {
		this(id, refConfiguration, decoderClass, encoderClass, null, refEventFactory);
	}	

	
	
	public long getInstanceNumber() {
		return instanceNumber;
	}

	static String createKeyNameForMultiplexer(NetworkCard networkCard) {
		
		String result = null;
		
		if (networkCard != null) {

			if (networkCard.getMacaddress() != null && !networkCard.getMacaddress().toLowerCase().equals("null")) {
				result = String.format("%s/%s", networkCard.getName(), networkCard.getMacaddress());
			} else {
				result = String.format("%s", networkCard.getName());
			}
			
		}

		return result;

	}
	
	static synchronized void createMultiplexer(Configuration refConfiguration, Class<? extends MessageDecoder<? extends Message>> decoderClass, Class<? extends MessageEncoder<? extends Message>> encoderClass, List<NetworkCard> listNetworkCards, SmartConcurrentHashMap<String, Multiplexer> mplexList, EventFactory refEventFactory) throws MultiplexerError {

		if (listNetworkCards != null && mplexList != null) {

			for (NetworkCard networkCard : listNetworkCards) {

				if (!networkCard.isUp()) {
					return;
				}
				
				String key = createKeyNameForMultiplexer(networkCard);

				Multiplexer multiplexer = mplexList.get(key);

				if (multiplexer == null) {

					multiplexer = new Multiplexer(key, refConfiguration, decoderClass, encoderClass, networkCard, refEventFactory);

					multiplexer.start();

					mplexList.put(key, multiplexer);

				}

			}
		}
		
	}
	
	static synchronized Multiplexer createMultiplexer(String keyName, Configuration refConfiguration, Class<? extends MessageDecoder<? extends Message>> decoderClass, Class<? extends MessageEncoder<? extends Message>> encoderClass, SmartConcurrentHashMap<String, Multiplexer> mplexList, EventFactory refEventFactory) throws MultiplexerError {

		Multiplexer multiplexer = null;
		
		if (keyName != null && mplexList != null) {

			multiplexer = mplexList.get(keyName);

			if (multiplexer == null) {

				multiplexer = new Multiplexer(keyName, refConfiguration, decoderClass, encoderClass, refEventFactory);

				multiplexer.start();

				mplexList.put(keyName, multiplexer);

			}

		}

		return multiplexer;

	}
	
	static synchronized Multiplexer searchForRelatedMultiplexer(final InetAddress localAddress, SmartConcurrentHashMap<String, Multiplexer> mplexList) {

		Multiplexer result = null;
		
		if (localAddress == null || localAddress.getHostAddress() == null || mplexList == null) {
			return null;
		}

		Iterator<String> it = mplexList.keySet().iterator();

		while (it.hasNext()) {

			String multiplexerKey = it.next();

			Multiplexer multiplexer = mplexList.get(multiplexerKey);

			NetworkCard nc = multiplexer.getNetworkCard();

			List<NetworkAddress> addresses = nc.getNetAddress();

			for (NetworkAddress a: addresses) {

				if (a.getHostAddress().equals(localAddress.getHostAddress())) {
					result = multiplexer;
					return result;
				};

			}

		}

		if (result == null) {
			result = mplexList.get(Multiplexer.FOR_UNKNOWN_SOCKET);
		}

		return result;

	}	

	Configuration getRefConfiguration() {
		return refConfiguration;
	}

	NetworkCard getNetworkCard() {
		return networkCard;
	}

	void setNetworkCard(NetworkCard networkCard) {
		this.networkCard = networkCard;
		
	}

	SmartConcurrentHashMap<SocketChannel, Session> getSessionList() {
		return sessionList;
	}

	void setSessionList(SmartConcurrentHashMap<SocketChannel, Session> sessionList) {
		this.sessionList = sessionList;
	}

	void register(Session session, SmartConcurrentHashMap<String, Multiplexer> refMultiplexerList) throws IOException {

		BasicSocket basicSocket = session.getBasicSocket();

		basicSocket.getSocketChannel().configureBlocking(false);
		basicSocket.setClientStatus(EnumConnectionStatus.StartedFull);
		basicSocket.setRefMultiplexer(this);
		basicSocket.getSocketChannel().register(this.selector, SelectionKey.OP_READ);

		sessionList.put(basicSocket.getSocketChannel(), session);
		
		plusSession();
		
		logger.trace(session, "A new socket has been registered by the multiplexer. Number of connections: %d", numberOfSessions);

	}
	
	
	SmartConcurrentLinkedQueue<Message> getOutputMessageQueue() {
		return outputMessageQueue;
	}

	void setOutputMessageQueue(SmartConcurrentLinkedQueue<Message> outputMessageQueue) {
		this.outputMessageQueue = outputMessageQueue;
	}

	@Override
	protected void doWorkAndRepeat() throws IOException, Throwable {

		// Ignora se o servidor não estiver ligado
		if (!this.started) {
			return;
		}

		try {

			/**************************
			 *  Processa fila de envio
			 **************************/
			try {

				Message sendMessage = null;

				// Acessa fila somente se existir mensagem
				if (this.outputMessageQueue.size() > 0) {
					sendMessage = this.outputMessageQueue.peek(); // Obtem elemento, mas não remove da fila
				}

				if (sendMessage != null) {

					boolean alreadyToClean = false;

					// Deve atender: ter uma sessão, ter socket, estar lógicamente ativo, mensagem deve ter conteúdo
					if (sendMessage.getSession() != null && sendMessage.getSession().getBasicSocket() != null
							&& sendMessage.getSession().getBasicSocket().getClientStatus().getValue() != EnumConnectionStatus.Done.getValue() && sendMessage.getContent() != null) {

						BasicSocket basicSocket = sendMessage.getSession().getBasicSocket();

						// Prepara e verifica se é momento de enviar a mensagem
						// Em caso de não preparado, não pode apagar a mensagem por aqui
						if (preparedForSend(basicSocket, sendMessage)) {						

							// Remove da fila
							//this.outputMessageQueue.remove(sendMessage);

							try {

								ByteBuffer buf = basicSocket.getEncoder().encode(sendMessage);

								//logger.trace("Mensagem após encode: %s", Function.byteBufferToString(buf));
								
								send(basicSocket.getSocketChannel(), buf);

								alreadyToClean = true;

							} catch (EncoderError e) {
								logger.error(sendMessage.getSession(), "Falha ao decodificar mensagem para envio. Erro: %s", ExceptionUtils.rootCauseMessage(e));
							}

						}

					} else {
						alreadyToClean = true;
					}

					// Verifica se está pronto para limpar mensagem
					if (alreadyToClean) {

						// Remove da fila
						this.outputMessageQueue.remove(sendMessage);
						
						// Descarta mensagem
						if (sendMessage.getContent() != null) {
							sendMessage.getContent().clear();
							sendMessage.setContent(null);
							sendMessage = null;
						}
						
					}

					
				}

			} catch (Exception e) {
				logger.error("Falha desconhecida ao processar fila de envio.");
			}

			/*******************************
			 *  Processa pendencias de alteração
			 *******************************/
			Iterator<SmartTimeObject<ChangeRequest>> changes = this.pendingChanges.iterator();

			while (changes.hasNext()) {

				SmartTimeObject<ChangeRequest> timeObject = changes.next(); 
				ChangeRequest change = timeObject.getValue();

				switch (change.type) {
					case ChangeRequest.CHANGEOPS:
						SelectionKey key = change.socket.keyFor(this.selector);
						if (key !=  null && key.isValid()) {
							key.interestOps(change.ops);
						}
				}
				
				// Dorme rápido
				fastSleep();

			}

			this.pendingChanges.clear();

			/**************************
			 *  Trata nova demanda do seletor
			 **************************/
			if (this.selector.selectNow() > 0) {

				// Iteração sobre chaves com eventos disponíveis
				Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();

				while (selectedKeys.hasNext()) {

					SelectionKey key = selectedKeys.next();
					selectedKeys.remove();

					if (key == null || !key.isValid()) {
						continue;
					}					

					if (key.isReadable()) {
						read(key);
					}
					else if (key.isWritable()) {
						write(key);
					}

					// Dorme rápido
					fastSleep();

					//prepareMessage(key);

				}

			}

			/***************************************
			 *  Verificação de conexão client 
			 ***************************************/
			Session session = this.sessionList.next();

			if (session != null) {

				prepareMessage(session);

				BasicSocket basicSocket = session.getBasicSocket();

				if (basicSocket != null && basicSocket.getNextVerifyTime().getMillis() < DateTime.now().getMillis() || !basicSocket.isActive()) {

					SelectionKey key = null;

					if (basicSocket.getSocketChannel() != null) {
						key = basicSocket.getSocketChannel().keyFor(this.selector);
					}

					this.checkTerminalConnection(session, basicSocket, key);

					basicSocket.updateNextVerifyTime();

				}

			}

		}
		catch (Exception e) {
			logger.error("Server error: %s", ExceptionUtils.rootCauseMessage(e));
		}
		catch (Throwable e) {
			logger.fatal("Server error: %s", ExceptionUtils.rootCauseMessage(e));
		}

	}

	public boolean preparedForSend(BasicSocket basicSocket, Message sendMessage) {

		boolean result = false;

		// Avalia condições de tempo para envio
		// Condição 1: Não há intervalo entre mensagens a ser obedecido
		// Condição 2: Obedecer intervalo para realizar envio (timeout para próximo envio é verificado) 
		if (basicSocket.getPermissionTimeToSend() == null || (basicSocket.getPermissionTimeToSend() != null && basicSocket.getPermissionTimeToSend().getMillis() < DateTime.now().getMillis())) {

			// Verifica se a mensagem requer alguma tarefa posterior
			if (sendMessage.getType() == EnumMessageType.SendAndConfirm || sendMessage.getType() == EnumMessageType.SendAndReceive || sendMessage.getType() == EnumMessageType.SendFuture) {

				if (!basicSocket.isSyncModeOn()) {

					basicSocket.turnSyncModeOn(sendMessage.getSyncCode(), sendMessage.getType(), sendMessage.getSynchTimeoutInSeconds());
					basicSocket.setSyncMessageToSend(Message.copy(sendMessage, encoderMessageType));
					
					result = true;
					//logger.trace(sendMessage.getSession(), "Turn sync mode on (timeout %d seconds)", sendMessage.getSynchTimeoutInSeconds());
				}
				// Se ocorrer timeout do sincronismo vigente, desliga modo sï¿½ncrono para possibilitar novos envios
				else if (basicSocket.getSynchTimeout() != null && basicSocket.getSynchTimeout().getMillis() < DateTime.now().getMillis()) {
					basicSocket.turnSyncModeOff();
					//logger.warn(sendMessage.getSession(), "Turn sync mode off by timeout");
				}

			} else {
				result = true;
			}

		}

		return result;

	}

	public boolean notifySendResult(Session session, BasicSocket basicSocket, boolean sent, ByteBuffer messageBuffer) {

		boolean result = false;

		// Verifica se a mensagem requer alguma tarefa posterior (aplicado em caso de confirmação)
		if (basicSocket.isSyncModeOn() && basicSocket.getSendSyncType() != null && (basicSocket.getSendSyncType().getValue()== EnumMessageType.SendAndConfirm.getValue() || basicSocket.getSendSyncType().getValue() == EnumMessageType.SendFuture.getValue())) {

			basicSocket.getSyncReceiveConfirmationQueue().put(basicSocket.getCurrentSyncCode(), sent);
			
			int requestSize = 0;
			Message requestCopied = null;
			
			if (basicSocket.getSyncMessageToSend() != null && basicSocket.getSyncMessageToSend().getContent() != null) {

				requestSize = basicSocket.getSyncMessageToSend().getContent().capacity();
				basicSocket.getSyncMessageToSend().getContent().position(0);
				requestCopied = Message.copy(basicSocket.getSyncMessageToSend(), encoderMessageType); 

			}

			// notifica envio após ter enviado a resposta síncrona para possibilitar manipulação de log via implementação do negócio
			refEventFactory._messageSent(session, requestSize, requestCopied);

			basicSocket.turnSyncModeOff();
			
			result = true;

			//logger.trace(session, "Turn sync mode off");

		} else {

			if (refEventFactory != null) {

				if (sent) {
					
					Message requestCopied = null;

					if (messageBuffer != null) {

						SmartBuffer sb = new SmartBuffer(messageBuffer.capacity());
					
						byte[] auxBuffer = new byte[messageBuffer.capacity()];

						messageBuffer.position(0);

						messageBuffer.get(auxBuffer, 0, messageBuffer.capacity());

						try {
							sb.write(auxBuffer);
						} catch (IOException e) {
							logger.error("Create smart buffer instance by decode failed. Error: %s", ExceptionUtils.rootCauseMessage(e));
							try {
								sb.close();
							} catch (Exception e1) {
								sb = null;
							}
							sb = null;
						}

						Message responseAux = null;
						
						MessageDecoder<? extends Message> refDecoder = session.getBasicSocket().getDecoder();
						
						if (sb != null) {
							try {
								responseAux = refDecoder.decode(sb);
							} catch (DecoderError e) {
								logger.error("Create request message instance by decode failed. Error: %s", ExceptionUtils.rootCauseMessage(e));
							}
						}

						if (responseAux != null) {

							try {
		
								requestCopied = (Message) encoderMessageType.newInstance();
								requestCopied.setSession(session);
								requestCopied.setContent(Function.copyByteBuffer(responseAux.getContent()));
		
								//System.err.println("Buffer copiado para evento _messageSent: " + Function.byteBufferToString(requestCopied.getContent(), true));

								refEventFactory._messageSent(session, responseAux.getContent().capacity(), requestCopied);

								result = true;
								
							} catch (InstantiationException | IllegalAccessException e) {
								logger.error("Create request message instance by generic failed. Error: %s", ExceptionUtils.rootCauseMessage(e));
							} 					
						
						}

					}
						
				}

			}			

		}

		return result;

	}	
	
	public boolean notifySendResult(Session session, BasicSocket basicSocket, Message message) {

		boolean result = false;

		// Verifica se a mensagem requer alguma tarefa posterior (aplicado em caso de mensagem de resposta)
		if (basicSocket.isSyncModeOn() && basicSocket.getSendSyncType() != null && basicSocket.getSendSyncType().getValue() == EnumMessageType.SendAndReceive.getValue()) {

			message.setType(EnumMessageType.ReceiveSych);

			message.setSyncCode(basicSocket.getCurrentSyncCode());

			basicSocket.getSyncReceiveMessageQueue().put(basicSocket.getCurrentSyncCode(), message);

			int responseSize = 0;
			Message responseMessage = null;
			
			if (message != null && message.getContent() != null) {

				responseSize = message.getContent().capacity();
				responseMessage = Message.copy(message, decoderMessageType);

				//responseMessage.setType(EnumMessageType.ReceiveSych);
				//responseMessage.setSyncCode(basicSocket.getCurrentSyncCode());

			}

			// Notifica envio após ter enviado a resposta síncrona para possibilitar manipulação de log via implementação do negócio
			refEventFactory._messageReceived(session, responseSize, responseMessage);			

			basicSocket.turnSyncModeOff();
			
			result = true;

			//logger.trace(session, "Turn sync mode off");

		} else {

			if (refEventFactory != null) {

				Message responseMessage = null;
				responseMessage = Message.copy(message, decoderMessageType);

				//message.setType(EnumMessageType.ReceiveOnly);
				
				refEventFactory._messageReceived(session, message.size(), responseMessage);

				result = true;

			}
			
		}

		return result;

	}		
	
	public void send(SocketChannel socket, ByteBuffer data) {

		// Adiciona intenção de escrita na fila de alterações de socket
		this.pendingChanges.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

		// Obtem fila de saída do socket
		ArrayList<ByteBuffer> boardingQueue = this.pendingData.get(socket);
		
		// Se não existir, cria e vincula a fila geral
		if (boardingQueue == null) {
			boardingQueue = new ArrayList<ByteBuffer>();
			this.pendingData.put(socket, boardingQueue);
		}

		// Adiciona dados a fila de saída do socket
		boardingQueue.add(data);

		this.selector.wakeup();

	}
		
	private void write(SelectionKey key) throws IOException {

		SocketChannel socketChannel = (SocketChannel) key.channel();

		if (socketChannel == null) {
			key.cancel();
			return;
		}
		
		Session session = this.sessionList.get(socketChannel);

		if (session == null) {
			key.cancel();
			return;
		}
		
		BasicSocket basicSocket = session.getBasicSocket();
		
		boolean hasSendError = false;
		
		// Verifica se o terminal é conhecido 
		if (basicSocket == null) {
			logger.error("A conexão %s é desconhecida e será descartada no envio de bytes.", socketChannel.socket().getInetAddress().getHostAddress());
			try {
				this.sessionList.remove(socketChannel);
				socketChannel.close();
				session.close();
			} catch (Exception e) {
				socketChannel = null;
				session = null;
			}
			return;
		}

		// Verifica desconexão do terminal
		if (!socketChannel.isConnected()) {
			return;
		}		

		ArrayList<ByteBuffer> boardingQueue = this.pendingData.get(socketChannel);

		if (
				(
					basicSocket.getClientStatus().getValue() < EnumConnectionStatus.ReadyToRemove.getValue() 
					|| (
						basicSocket.getClientStatus().getValue() == EnumConnectionStatus.ReadyToRemove.getValue() 
						&& basicSocket.getTimeToDestroy() != null && basicSocket.getTimeToDestroy().getMillis() >= DateTime.now().getMillis()
						&& basicSocket.getDisconnectReason().getValue() != EnumDisconnectionReason.SendError.getValue()
						&& basicSocket.getDisconnectReason().getValue() != EnumDisconnectionReason.ReceiveError.getValue()
						&& basicSocket.getDisconnectReason().getValue() != EnumDisconnectionReason.HostUnplugged.getValue()
					)
				)
				&& boardingQueue != null
			) { 

			int numWrite = 0;
			ByteBuffer buf = null;

			while (!boardingQueue.isEmpty()) {

				buf = boardingQueue.get(0);  

				try {

					if (!hasSendError) {

						numWrite = socketChannel.write(buf);

						// Atualiza parâmetro de limite para próximo envio
						// Utilizado em caso de conexÃµes críticas como GPRS
						basicSocket.setPermissionTimeToSend(refConfiguration.getSendDelayByConnectionMillis());

						if (numWrite < 0) {
							notifySendResult(session, basicSocket, false, null);
							throw new IOException("O host encerrou a conexão.");
						}
						else if (buf.remaining() == 0){
							basicSocket.setLastMessageTime(DateTime.now());
							ByteBuffer copyBuff = Function.copyByteBuffer(buf);
							notifySendResult(session, basicSocket, true, copyBuff);
							logger.trace(session, "Mensagem enviada (%d bytes): %s", buf.capacity(), Function.byteBufferToString(buf, true));
						}

					}

				} catch (IOException e) {
					e.printStackTrace();
					hasSendError = true;
					String errorS = ExceptionUtils.rootCauseMessage(e);
					if (errorS == null) {
						logger.warn(session, "Falha ao enviar buffer para o socket. A conexão foi encerrada pelo host.");
					} else {
						logger.warn(session, "Falha ao enviar buffer para o socket. Erro: %s", errorS);
					}
					basicSocket.prepareToDisconnect(EnumDisconnectionReason.HostUnplugged);
				} catch (Throwable e) {
					e.printStackTrace();
					hasSendError = true;
					logger.warn(session, "Falha ao enviar buffer para o socket. A conexão foi encerrada pelo host.");
					basicSocket.prepareToDisconnect(EnumDisconnectionReason.HostUnplugged);
				}

				// Se existir bytes pendentes, envia continua envio numa próxima tentativa
				// porque o socket deve estar com o buffer cheio
				if (!hasSendError && buf.remaining() > 0) {
					continue;
				} else if (hasSendError) {
					break;
				}

				boardingQueue.remove(0);

				buf.clear();
				
				// Dorme rápido
				fastSleep();

			}

			if (hasSendError) {
				// Limpa mensagens em fila porque é impossível envia-las para o socket aqui
				if (boardingQueue != null) {
					while (!boardingQueue.isEmpty()) {
						buf = boardingQueue.get(0);
						if (buf != null) {
							buf.clear();
							buf = null;
						}
						boardingQueue.remove(0);
					}
				}
			}			
			
			if (boardingQueue.isEmpty()) {
				key.interestOps(SelectionKey.OP_READ);
			}

		} else {
			key.interestOps(SelectionKey.OP_READ);
		}

	}	
	
	private void read(SelectionKey key) throws IOException {

		SocketChannel socketChannel = (SocketChannel) key.channel();

		/*********************************************
		 * Verifica condições para recebimento
		 *********************************************/
		
		if (socketChannel == null) {
			logger.warn("O evento de recebimento de bytes não possui uma conexão vinculada e por isso foi descatado.");
			key.cancel();
			return;
		}

		Session session = this.sessionList.get(socketChannel);

		if (session == null) {
			try {
				socketChannel.close();
			} catch (IOException e) {
				socketChannel = null;
			}
			key.cancel();
			return;
		}

		BasicSocket basicSocket = session.getBasicSocket();

		// Verifica se o terminal é conhecido 
		if (basicSocket == null) {
			logger.warn("A conexão %s é desconhecida e sua sessão foi removida durante o recebimento de bytes.", socketChannel.socket().getInetAddress().getHostAddress());
			try {
				this.sessionList.remove(socketChannel);
				socketChannel.close();
				session.close();
			} catch (Exception e) {
				socketChannel = null;
				session = null;
			}
			return;
		}

		// Verifica sea conexão está ativa para leitura
		if (!basicSocket.isActive()) {
			return;
		}		
		
		// Verifica desconexão do terminal
		if (!socketChannel.isConnected()) {
			return;
		}		

		this.readBuffer.clear();

		int numRead = 0;

		try {

			numRead = socketChannel.read(this.readBuffer);

		} catch (IOException e) {

			numRead = -1;
			logger.error(session, "Falha ao ler conexão. Erro: %s", ExceptionUtils.rootCauseMessage(e));
			//basicSocket.prepareToDisconnect(EnumDisconnectionReason.ReceiveError);

		} catch (Throwable e) {

			numRead = -1;
			logger.error(session, "Falha ao ler conexão.");
			//basicSocket.prepareToDisconnect(EnumDisconnectionReason.ReceiveError);

		}

		if (numRead == -1) {

			logger.warn(session, "O host encerrou a conexão.");
			basicSocket.prepareToDisconnect(EnumDisconnectionReason.HostUnplugged);


		} else if (numRead > 0) { // Aqui só entra se tiver algo lido

			// Tempo da última mensagem recebida
			basicSocket.setLastMessageTime(DateTime.now());

			this.readBuffer.flip();
			this.readBuffer.position(0);

			byte [] bytes = new byte[numRead]; // Buffer do tamanho do que foi recebido
			this.readBuffer.get(bytes, 0, bytes.length);

			//logger.trace(basicSocket, ">>> Receive area antes: %d", basicSocket.getReceiveArea().getSize());
			basicSocket.getReceiveArea().write(bytes);
			//logger.trace(basicSocket, ">>> Receive area depois: %d", basicSocket.getReceiveArea().getSize());

			this.readBuffer.position(0);

			logger.trace(session, "Recebido buffer: %s", Function.byteArrayToHex(bytes, true)); // Print daquele buffer

			//logger.trace(session, "Armazenado: %d bytes", basicSocket.getReceiveArea().getSize()); // Print daquele buffer

		}

	}
	
	private void prepareMessage(Session session) throws IOException {

		// Verifica se o terminal é conhecido 
		if (session == null || session.getBasicSocket() == null) {
			return;
		}

		BasicSocket basicSocket = session.getBasicSocket();
		
		if (!basicSocket.isActive() || basicSocket.getReceiveArea() == null || basicSocket.getReceiveArea().getSize() == 0) {
			return;
		}

		try {

			while (true) {

				//logger.trace(basicSocket, ">>> Receive area PRÃ‰ decode: %d", basicSocket.getReceiveArea().getSize());
				Message message = basicSocket.getDecoder().decode(basicSocket.getReceiveArea());
				//logger.trace(basicSocket, ">>> Receive area PÃ“S decode: %d", basicSocket.getReceiveArea().getSize());

				if (message != null) {

					message.setSession(session);

					if (!notifySendResult(session, basicSocket, message)) {
						logger.warn(session, "A mensagem recebida será descartada porque não foi possível entregar a um destinatário. Mensagem: %s", Function.byteBufferToString(message.getContent(), true));
						message.close();
					};

				} else {
					break;
				}

				// Dorme rápido
				this.fastSleep();

			}

		} catch (Exception e) {

			if (basicSocket.getNumMessageReceive() == 0L) {

				logger.warn(session, "Falha ao ler socket. A conexão será encerrada por não ter recebido nenhuma mensagem. Erro: %s", ExceptionUtils.rootCauseMessage(e));

			} else {

				logger.warn(session, "Falha ao ler socket. Erro: %s", ExceptionUtils.rootCauseMessage(e));

			}

			basicSocket.prepareToDisconnect(EnumDisconnectionReason.MessagePreparationError);
			
		} catch (Throwable e) {

			logger.warn("Falha ao ler socket. A conexão será encerrada. Error: %s", ExceptionUtils.rootCauseMessage(e));

			basicSocket.prepareToDisconnect(EnumDisconnectionReason.MessagePreparationError);

		}
			
	}		
	
	private void checkTerminalConnection(Session session, BasicSocket basicSocket, SelectionKey key) {

		SocketChannel socketChannel = null;

		try {

			// Se a conexão não tiver pronta, ou em processo de aceite, ou estiver em processo de encerramento, não monitorar
			if (basicSocket == null || basicSocket.getClientStatus().getValue() <= EnumConnectionStatus.Stopped.getValue()) {
				return;
			}

			socketChannel = basicSocket.getSocketChannel();

			// ONLINE
			if (basicSocket.isActive()) {

				if (refConfiguration.getMode().getValue() == EnumConfigurationMode.Server.getValue()) {
				
					ServerConfiguration config = (ServerConfiguration) refConfiguration;  
					
					// Verifica timeout de envio da primeira mensagem
					if (config.getFirstMessageTimeout() > 0 && basicSocket.getLastMessageTime() == null && basicSocket.getStartDateTime() != null && basicSocket.getStartDateTime().plusSeconds(config.getFirstMessageTimeout()).getMillis() < DateTime.now().getMillis()) {
						basicSocket.prepareToDisconnect(EnumDisconnectionReason.FirstMessageTimeout);
						logger.info(session, "Atingido tempo máximo (%d segundos) de espera por primeira mensagem.", config.getFirstMessageTimeout());
					}
					// Verifica timeout de ociosidade
					else if (config.getIdleTimeout() > 0 && ((basicSocket.getLastMessageTime() == null? basicSocket.getStartDateTime(): basicSocket.getLastMessageTime()).plusSeconds(config.getIdleTimeout()).getMillis() < DateTime.now().getMillis())) {
						basicSocket.prepareToDisconnect(EnumDisconnectionReason.IdleTimeout);
						logger.info(session, "Atingido tempo máximo de ociosidade (%d segundos).", config.getIdleTimeout());
					}
					// Verifica timeout de reconexão
					else if (config.getReconnectionInterval() > 0 && basicSocket.getStartDateTime() != null && (basicSocket.getStartDateTime().plusSeconds(config.getReconnectionInterval()).getMillis() < DateTime.now().getMillis())) {
						basicSocket.prepareToDisconnect(EnumDisconnectionReason.ReconnectTimeout);
						logger.info(session, "Atingido tempo máximo para reconexão (%d segundos).", config.getReconnectionInterval());
					}
					// Verifica se a conexão perdeu registro
					else if (key == null) {
						basicSocket.prepareToDisconnect(EnumDisconnectionReason.HostUnplugged);
						logger.info(session, "A conexão não está registrada no seletor. Provavelmente foi abandonada porque a interface de rede ficou indisponível.");
						
					// Verifica latÃªncia da conexão
					}
					/**
					else if (this.checkTerminalLatency && (basicSocket.getLastLatencyCheckTime() == 0L || basicSocket.getLastLatencyCheckTime() < (System.currentTimeMillis() - pingTerminalInterval))) {
	
						basicSocket.setLastLatencyCheckTime(System.currentTimeMillis());
	
						ByteBuffer byteBuffer = ByteBuffer.wrap("checkLatency".getBytes());
	
						DGMessage<ByteBuffer> checkLatency = new DGMessage<ByteBuffer>(basicSocket, byteBuffer); 
	
						this.icmpTaskQueue.add(checkLatency);
	
					}*/

				}
				else if (refConfiguration.getMode().getValue() == EnumConfigurationMode.Client.getValue()) {
					
					ClientConfiguration config = (ClientConfiguration) refConfiguration;  
					
					// Verifica timeout de ociosidade
					if (config.getIdleTimeout() > 0 && ((basicSocket.getLastMessageTime() == null? basicSocket.getStartDateTime(): basicSocket.getLastMessageTime()).plusSeconds(config.getIdleTimeout()).getMillis() < DateTime.now().getMillis())) {
						basicSocket.prepareToDisconnect(EnumDisconnectionReason.IdleTimeout);
						logger.info(session, "Atingido tempo máximo de ociosidade (%d segundos).", config.getIdleTimeout());
					}
					// Verifica timeout de reconexão
					else if (config.getReconnectionInterval() > 0 && basicSocket.getStartDateTime() != null && (basicSocket.getStartDateTime().plusSeconds(config.getReconnectionInterval()).getMillis() < DateTime.now().getMillis())) {
						basicSocket.prepareToDisconnect(EnumDisconnectionReason.ReconnectTimeout);
						logger.info(session, "Atingido tempo máximo para reconexão (%d segundos).", config.getReconnectionInterval());
					}
					// Verifica se a conexão perdeu registro
					else if (key == null) {
						basicSocket.prepareToDisconnect(EnumDisconnectionReason.HostUnplugged);
						logger.info(session, "A conexão não está registrada no seletor. Provavelmente foi abandonada porque a interface de rede ficou indisponível.");
						
					// Verifica latÃªncia da conexão
					}
					/**
					else if (this.checkTerminalLatency && (basicSocket.getLastLatencyCheckTime() == 0L || basicSocket.getLastLatencyCheckTime() < (System.currentTimeMillis() - pingTerminalInterval))) {
	
						basicSocket.setLastLatencyCheckTime(System.currentTimeMillis());
	
						ByteBuffer byteBuffer = ByteBuffer.wrap("checkLatency".getBytes());
	
						DGMessage<ByteBuffer> checkLatency = new DGMessage<ByteBuffer>(basicSocket, byteBuffer); 
	
						this.icmpTaskQueue.add(checkLatency);
	
					}*/

				}				
				
				

			} 
			// OFFLINE
			else if (!basicSocket.isActive()) {

				if (basicSocket.getClientStatus().getValue() >= EnumConnectionStatus.Stopped.getValue() 
					&& basicSocket.getClientStatus().getValue() < EnumConnectionStatus.Done.getValue()
					&& basicSocket.getTimeToDestroy() != null && basicSocket.getTimeToDestroy().getMillis() < System.currentTimeMillis()) { 

					//this.refEventFactory._closedConnection(session, basicSocket.getDisconnectReason());
					
					// Informa o fechamento da conexão para novas requisições
					if (refConfiguration.getMode().getValue() == EnumConfigurationMode.Client.getValue()) {
						((ClientEventFactory) this.refEventFactory)._closedConnection(session, basicSocket.getDisconnectReason());
					} else {
						this.refEventFactory._closedConnection(session, basicSocket.getDisconnectReason());
					}
					
					basicSocket.setClientStatus(EnumConnectionStatus.Done);

				}

			}

			// Verifica se a conexão pode ser detruída
			if (basicSocket.getClientStatus().getValue() == EnumConnectionStatus.Done.getValue()
					&& basicSocket.getTimeToDestroy() != null
					&& basicSocket.getTimeToDestroy().getMillis() < DateTime.now().getMillis()) {
				
				// Limpa a fila de saída
				ArrayList<ByteBuffer> queue = this.pendingData.get(socketChannel);
				if (queue != null) {
					while (!queue.isEmpty()) {
						ByteBuffer buf = queue.get(0);
						if (buf != null) {
							buf.clear();
							buf = null;
						}
						queue.remove(0);

						// Dorme rápido
						this.fastSleep();

					}
				}

				this.sessionList.remove(socketChannel);

				this.refEventFactory._destroyedConnection(basicSocket.getId(), basicSocket.getHostaddress());

				if (session != null) {
					session.close();
				}

				logger.info("The connection %s has been removed from multiplexer. Number of connections: %d.", basicSocket.getHostaddress(), getNumberOfSession());

				session = null;
				
			}

		} catch (Throwable e) {
			if (basicSocket != null && basicSocket.isActive()) {
				logger.warn(session, "Falha ao monitorar socket. A conexão será encerrada. Erro: %s", ExceptionUtils.rootCauseMessage(e));
				basicSocket.prepareToDisconnect(EnumDisconnectionReason.ProcessError);
			}
		}

	}
	
	@Override
	protected void startEvent() {

		logger.trace("Started multiplexer %d - %s", super.getId(), super.getName());
		
	}

	@Override
	protected void pauseEvent() {

		logger.trace("Paused multiplexer %d - %s", super.getId(), super.getName());
		
	}

	@Override
	protected void stopEvent() {

		logger.trace("Stopped multiplexer %d - %s", super.getId(), super.getName());

	}

	@Override
	protected void resumeEvent() {

		logger.trace("Resumed multiplexer %d - %s", super.getId(), super.getName());
		
	}

	public void destroyObjects() {

		if (outputMessageQueue != null && outputMessageQueue.size() > 0) {
			outputMessageQueue.clear();
			outputMessageQueue = null;
		}
		
		if (pendingChanges != null && pendingChanges.size() > 0) {
			pendingChanges.clear();
			pendingChanges = null;
		}

		if (pendingData != null && pendingData.size() > 0) {
			pendingData.clear();
			pendingData = null;
		}
		
		boolean hasSession = false;
		
		if (sessionList != null) {

			// Faz o fechamento das conexï¿½es
			while (true) {
			
				Session session = sessionList.next();

				if (session == null) {
					break;
				}
				
				this.refEventFactory._closedConnection(session, EnumDisconnectionReason.ServerStopRequest);
				
				BasicSocket basicSocket = session.getBasicSocket();
				
				basicSocket.prepareToDisconnect(EnumDisconnectionReason.ServerStopRequest);

				if (!hasSession) {
					hasSession = true;
				}
				
			}

			if (hasSession) {
				try {
					SmartThread.sleep(SmartThread.ONESECOND_VALUE * 2);
				} catch (InterruptedException e) {
					logger.error("Error while destroyng multiplexer. Error: %s", ExceptionUtils.rootCauseMessage(e));
				}
			}

			
			// Faz o fechamento das conexï¿½es
			while (hasSession) {
			
				Session session = sessionList.next();

				if (session == null) {
					break;
				}
				
				this.refEventFactory._destroyedConnection(session.getBasicSocket().getId(), session.getBasicSocket().getHostaddress());

				try {

					session.close();
				
				} catch (Exception e) {
					logger.error("Error while destroying multiplexer. Error: %s", ExceptionUtils.rootCauseMessage(e));
				} catch (Throwable e) {
					logger.error("Error while destroying multiplexer. Error: %s", ExceptionUtils.rootCauseMessage(e));
				}
				
			}

			sessionList.clear();

		}
		
		if (selector != null) {
			if (selector.isOpen()) {
				try {
					selector.close();
				} catch (Exception e) {
				} catch (Throwable e) {
				}
			}
			selector = null;
		}
		
	}
	
	@Override
	public void close() throws Exception {

		try {

			super.stopWork();
			
			this.destroyObjects();

		} catch (Exception e) {
			logger.error("Error while closing multiplexer. Error: %s", ExceptionUtils.rootCauseMessage(e));
		} catch (Throwable e) {
			logger.error("Error while closing multiplexer. Error: %s", ExceptionUtils.rootCauseMessage(e));
		}
		
	}

}
