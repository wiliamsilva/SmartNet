package br.com.wps.smartnet.ssl;

import br.com.wps.smartnet.core.EnumConnectionStatus;
import br.com.wps.smartnet.core.EnumMessageType;
import br.com.wps.smartnet.exception.SendError;
import br.com.wps.smartnet.exception.WrapError;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnetutils.collection.SmartConcurrentHashMap;
import br.com.wps.smartnetutils.ext.ExceptionUtils;
import br.com.wps.smartnetutils.ext.GeneralExecutor;
import br.com.wps.smartnetutils.ext.SmartThread;

public class SSLSender {

	private SSLSession refSession;
	private SmartConcurrentHashMap<String, SSLMultiplexer> refMultiplexerList;

	protected SmartLog logger;
	
	private SSLSender() {
		this.logger = new SmartLog(this);
	}
	
	public SSLSender(SmartConcurrentHashMap<String, SSLMultiplexer> refMultiplexerList) {

		this();
		
		this.refMultiplexerList = refMultiplexerList;

	}

	public SSLSender(SSLSession refSession, SmartConcurrentHashMap<String, SSLMultiplexer> refMultiplexerList) {
		
		this(refMultiplexerList);

		this.refSession = refSession;

	}
	
	SSLSession getRefSession() {
		return refSession;
	}

	void setRefSession(SSLSession refSession) {
		this.refSession = refSession;
	}

	public String checkCondictionForSendingP2P(SSLMessage message) {
		
		String result = null;
		
		if (message == null) {
			
			result = String.format("A mensagem não será enviada porque é nula");

			if (this.refSession != null && this.refSession.getBasicSocket() != null) {
				logger.error(refSession, result);
			} else {
				logger.error(result);
			}

		}
		else if (this.refSession == null || this.refSession.getBasicSocket() == null || !this.refSession.getBasicSocket().isActive() || this.refSession.getBasicSocket().getRefMultiplexer() == null || this.refSession.getBasicSocket().getRefMultiplexer().getOutputMessageQueue() == null) {

			result = String.format("A mensagem não será enviada por indisponibilidade da conexão: %s", message.getHexaContent());
			
			if (this.refSession != null && this.refSession.getBasicSocket() != null) {
				logger.error(refSession, result);
			} else {
				logger.error(result);
			}

		}		

		return result;

	}

	public String checkCondictionForSendingToAll(SSLMessage message) {
		
		String result = null;
		
		if (message == null) {
			
			result = String.format("A mensagem não será enviada por porque é nula");

			logger.error(result);

		}
		else if (this.refMultiplexerList == null || this.refMultiplexerList.size() == 0) {

			result = String.format("A mensagem não será enviada por indisponibilidade da conexão: %s", message.getHexaContent());
			
			logger.error(result);

		}		

		return result;

	}
	
	
	//@Override
	public void send(SSLMessage message) throws SendError {

		// Envio simples
		
		String error = checkCondictionForSendingP2P(message);
		
		if (error != null) {
			throw new SendError(error);
		}

		message.setType(EnumMessageType.SendOnly);
		message.setSession(refSession);
		
		try {
			message.wrap();
		} catch (WrapError e) {
			logger.error(message.getSession(), "Wrap failed on send. Error: %s", ExceptionUtils.rootCauseMessage(e));
		}
		
		//logger.trace("Adicionou mensagem a lista de saída");
		
		refSession.getBasicSocket().getRefMultiplexer().getOutputMessageQueue().add(message);

	}

	public boolean sendAndConfirm(SSLMessage message) throws SendError {
		return sendAndConfirm(message, refSession.getRefConfig().getWaitTimeout());
	}
	
	//@Override
	public boolean sendAndConfirm(SSLMessage message, int waitTimeout) throws SendError {

		// Envio e confirmação
		
		boolean result = false;
		
		String error = checkCondictionForSendingP2P(message);
		
		if (error != null) {
			throw new SendError(error);
		}

		long syncCode = SSLMessage.nextSyncCode();
		
		message.setSyncCode(syncCode);
		message.setType(EnumMessageType.SendAndConfirm);
		message.setSynchTimeoutInSeconds(waitTimeout);
		message.setSession(refSession);

		try {
			message.wrap();
		} catch (WrapError e) {
			logger.error(message.getSession(), "Wrap failed on send with confirm. Error: %s", ExceptionUtils.rootCauseMessage(e));
		}
		
		refSession.getBasicSocket().getRefMultiplexer().getOutputMessageQueue().add(message);
		
		long timeout = System.currentTimeMillis() + (waitTimeout * 1000L);

		SmartConcurrentHashMap<Long, Boolean> syncReceiveConfirmationQueue = refSession.getBasicSocket().getSyncReceiveConfirmationQueue();
		Boolean response = null;
		
		while (true) {

			if (timeout < System.currentTimeMillis()) {
				throw new SendError(String.format("Atingido o tempo de espera (%d s) por confirmação.", waitTimeout));
			}

			response = syncReceiveConfirmationQueue.get(syncCode);

			if (response != null) {
				syncReceiveConfirmationQueue.remove(syncCode);
				break;
			}

			try {
				Thread.sleep(SmartThread.FAST_SLEEP_VALUE);
			} catch (InterruptedException e) {
				throw new SendError(String.format("Falha ao aguardar confirmação de envio na comunicação síncrona. Erro: %s", ExceptionUtils.rootCauseMessage(e)));
			}

			if (this.refSession.getBasicSocket().getClientStatus().getValue() == EnumConnectionStatus.Done.getValue()) {
				throw new SendError("O envio de mensagem com espera por confirmação não pôde ser executado porque a conexão foi inativada.");
			}			

		}
		
		if (response != null) {
			result = response.booleanValue();
		}

		return result;

	}

	public SSLMessage sendAndReceive(SSLMessage message) throws SendError {
		return sendAndReceive(message, refSession.getRefConfig().getWaitTimeout());
	}

	//@Override
	public SSLMessage sendAndReceive(SSLMessage message, int waitTimeout) throws SendError {

		// Envio e espera uma resposta
		
		SSLMessage result = null;
		
		// Verificação de condições para envio
		String error = checkCondictionForSendingP2P(message);
		
		if (error != null) {
			throw new SendError(error);
		}

		// Preenche dados para controlar o envio/recebimento em série
		long syncCode = SSLMessage.nextSyncCode();

		message.setSyncCode(syncCode);
		message.setType(EnumMessageType.SendAndReceive);
		message.setSynchTimeoutInSeconds(waitTimeout);
		message.setSession(refSession);

		try {
			message.wrap();
		} catch (WrapError e) {
			logger.error(message.getSession(), "Wrap failed on send with response. Error: %s", ExceptionUtils.rootCauseMessage(e));
		}
		
		// Adiciona mensagem na fila de saída do multiplexer
		refSession.getBasicSocket().getRefMultiplexer().getOutputMessageQueue().add(message);
		
		long timeout = System.currentTimeMillis() + (waitTimeout * 1000L);
		
		SmartConcurrentHashMap<Long, SSLMessage> syncReceiveMessageQueue = refSession.getBasicSocket().getSyncReceiveMessageQueue();
		
		while (true) {

			if (timeout < System.currentTimeMillis()) {
				throw new SendError(String.format("Atingido o tempo de espera (%d s) por mensagem de resposta.", waitTimeout));
			}

			result = syncReceiveMessageQueue.get(syncCode);

			if (result != null) {
				syncReceiveMessageQueue.remove(syncCode);
				break;
			}

			try {
				Thread.sleep(SmartThread.FAST_SLEEP_VALUE);
			} catch (InterruptedException e) {
				throw new SendError(String.format("Falha ao aguardar resposta de requisição na comunicação síncrona. Erro: %s", ExceptionUtils.rootCauseMessage(e)));
			}

			if (this.refSession.getBasicSocket().getClientStatus().getValue() == EnumConnectionStatus.Done.getValue()) {
				logger.warn(refSession, "O envio de mensagem com espera por resposta não pÃªde ser executado porque a conexão foi inativada.");
				break;
			}			

		}

		return result;		

	}

	public void send(SSLMessage message, Runnable successHandler, Runnable failureHandler) throws SendError {
		send(message, refSession.getRefConfig().getWaitTimeout(), successHandler, failureHandler);
	}
	
	//@Override
	public void send(SSLMessage message, int waitTimeout, Runnable successHandler, Runnable failureHandler) throws SendError {

		// Envio e confirmação seguida de evento futuro para sucesso ou falha
		
		boolean result = sendAndConfirm(message, waitTimeout);

		if (result) {
			GeneralExecutor.execute(successHandler);
		} else {
			GeneralExecutor.execute(failureHandler);
		}

	}

}
