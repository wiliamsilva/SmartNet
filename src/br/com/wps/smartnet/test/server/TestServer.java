package br.com.wps.smartnet.test.server;

import br.com.wps.smartnet.core.ServerConfiguration;
import br.com.wps.smartnet.core.SmartServer;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnetutils.ext.ExceptionUtils;
import br.com.wps.smartnetutils.ext.SmartThread;
import br.com.wps.smartnetutils.ext.BasicLog;

public class TestServer {

	private static SmartLog logger;
	
	public static void main(String[] args) {
		
		System.setProperty(BasicLog.CONFIGURATION_FILE_SYSTEM_PROPERTY, "C:/Users/wiliam/workspace/SmartNet/server_log4j2.xml");
		
		ServerConfiguration configuration = new ServerConfiguration();
		configuration.setMaxConnections(10);
		configuration.setFirstMessageTimeout(0);
		configuration.setWaitTimeout(30);
		configuration.setIdleTimeout(0);
		configuration.setReconnectionInterval(0);

		String addressParam = "127.0.0.1 9001; ::1 9002; 192.168.0.173 9010";

		SmartServer server = null;

		try {
			
			server = new SmartServer("server", configuration, DecoderTest.class, EncoderTest.class, new EventFactory());

			logger = new SmartLog(server);
			

			logger.info("Teste de log");
			
			server.bind(addressParam);
			
			long timeout = (long) (2.5 * 60L * 60L * 1000L) + System.currentTimeMillis();
			
			while (timeout > System.currentTimeMillis()) {
				
				Thread.sleep(SmartThread.LONG_SLEEP_VALUE);
				
			}
			
			server.close();
			
			
		} catch (Exception e) {
			logger.error("Falha durante teste de servidor. Erro: %s", ExceptionUtils.rootCauseMessage(e));;
		}
		
	
		
	}
	
	
}
