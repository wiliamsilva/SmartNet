package br.com.wps.smartnet.test.sslserver;

import br.com.wps.smartnet.core.ServerConfiguration;
import br.com.wps.smartnet.exception.BindError;
import br.com.wps.smartnet.exception.InvalidParameterError;
import br.com.wps.smartnet.exception.ServerError;
import br.com.wps.smartnet.exception.UnexpectedError;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnet.ssl.SSLSmartServer;
import br.com.wps.smartnetutils.ext.SmartThread;
import br.com.wps.smartnetutils.ext.BasicLog;

public class SSLTestServer {

	private static SmartLog logger;
	
	public static void main(String[] args) {
		

		System.setProperty(BasicLog.CONFIGURATION_FILE_SYSTEM_PROPERTY, "C:/Users/wiliam/workspace/SmartNet/server_log4j2.xml");
		
		ServerConfiguration configuration = new ServerConfiguration();
		configuration.setMaxConnections(10);
		configuration.setFirstMessageTimeout(0);
		configuration.setWaitTimeout(30);
		configuration.setIdleTimeout(0);
		configuration.setReconnectionInterval(0);

		configuration.setTrustStoreFile("C:\\teste\\pki-example-2\\ca\\truststore.jks");
		configuration.setTrustStorePassword("abc123456");
		configuration.setKeyStoreFile("C:\\teste\\pki-example-2\\certs\\server.jks");
		configuration.setKeyStorePassword("abc123456");
		configuration.setKeyPassword("324Rc13Vk26GOsKR887Z");
		configuration.setMaxSSLMessageProcess(50);
		
		configuration.setKeyPassword("9m3610l3BLv4k38q7096");

		String addressParam = "127.0.0.1 9001; ::1 9002; 192.168.0.173 9010";

		SSLSmartServer server = null;

		try {
			
			server = new SSLSmartServer("server", configuration, DecoderTest.class, EncoderTest.class, new EventFactory());

			logger = new SmartLog(server);
			

			logger.info("Teste de log");
			
			server.bind(addressParam);
			
			long timeout = (((2 * 60 * 60) + 240) * 1000L) + System.currentTimeMillis();
			
			while (timeout > System.currentTimeMillis()) {
				
				Thread.sleep(SmartThread.LONG_SLEEP_VALUE);
				
			}
			
			server.close();
			
			
		} catch (InvalidParameterError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnexpectedError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BindError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServerError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
		
	}
	
	
}
