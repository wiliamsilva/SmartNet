package br.com.wps.smartnet.test.client;

import java.io.IOException;

import br.com.wps.smartnet.core.ClientEventFactory;
import br.com.wps.smartnet.core.EnumDisconnectionReason;
import br.com.wps.smartnet.core.Message;
import br.com.wps.smartnet.core.Session;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnetutils.ext.ExceptionUtils;

public class EventFactorySimple extends ClientEventFactory {

	private SmartLog logger;

	public EventFactorySimple() {
		logger = new SmartLog(this);
	}

	@Override
	public void starting() {
		
		logger.info("Iniciando o client");

	}

	@Override
	public void started() {

		logger.info("Client iniciado com sucesso");

	}

	@Override
	public void connected(Session session) {

		logger.info(session, "Conexão realizada com sucesso.");
		
	}

	@Override
	public void rejectedConnection(Session session, EnumDisconnectionReason reason) {

		logger.error(session, "Conexão rejeitada");

	}

	@Override
	public void closedConnection(Session session, EnumDisconnectionReason reason) {

		logger.info(session, "Conexão encerrada: %s", reason.toString());

	}

	@Override
	public void destroyedConnection(long connId, String address) {

		logger.trace("Conexão destruída: %d - %s", connId, address);
		
	}

	@Override
	public void connectionError(Session session, IOException e) {

		logger.error(session, "Falha na conexão: %s", ExceptionUtils.rootCauseMessage(e));

	}

	@Override
	public void messageReceived(Session session, int bytesReceived, Message message) {

		if (bytesReceived <= 0 || message == null) {
			logger.warn(session, "Mensagem recebida: [nula]");
			return;
		}

		MessageTest decodedMessage = null;
		
		if (message instanceof MessageTest) {
			decodedMessage = (MessageTest) message; 
		} else {
			logger.warn(session, "Tipo de mensagem desconhecido: %s", message.getClass().getSimpleName());
			return;
		}

		logger.info(session, "Mensagem recebida: %s", decodedMessage.getText());

	}

	@Override
	public void messageSent(Session session, int bytesSent, Message message) {

		if (bytesSent <= 0 || message == null) {
			logger.warn(session, "Mensagem enviada: [nula]");
			return;
		}

		MessageTest decodedMessage = null;

		if (message instanceof MessageTest) {
			decodedMessage = (MessageTest) message; 
		} else {
			logger.warn(session, "Tipo de mensagem desconhecido: %s", message.getClass().getSimpleName());
			return;
		}

		logger.info(session, "Mensagem enviada: %s", decodedMessage.getText());
		
	}

	@Override
	public void error(IOException e) {

		logger.error("Erro no client: %s", ExceptionUtils.rootCauseMessage(e));

	}

	@Override
	public void stoping() {

		logger.info("Encerrando o client");
		
	}

	@Override
	public void stopped() {

		logger.info("Client encerrado com sucesso");
		
	}

}
