package br.com.wps.smartnet.logger;

import org.apache.logging.log4j.Level;

import br.com.wps.smartnet.core.BasicSocket;
import br.com.wps.smartnet.core.EnumConnectionType;
import br.com.wps.smartnet.core.Session;
import br.com.wps.smartnet.ssl.SSLBasicSocket;
import br.com.wps.smartnet.ssl.SSLSession;
import br.com.wps.smartnetutils.ext.BasicLog;
import br.com.wps.smartnetutils.valueobject.IPAddress;

public class SmartLog extends BasicLog {
	
	public SmartLog(Object invokerObject) {

		super(invokerObject);
		
	}
	
	public static String getEnvelopeHead(Session session) {

		StringBuffer sb = new StringBuffer();
		
		if (session != null && session.getBasicSocket() != null) {
		
			BasicSocket basicSocket = session.getBasicSocket();
			
			if (basicSocket.getPrefix4Log() != null) {

				sb.append("["); sb.append(basicSocket.getPrefix4Log()); 
				sb.append("] ");

			} 
			
			sb.append("[h."); sb.append(basicSocket.getHostaddress());
			if (basicSocket.getConnectionType() != null && basicSocket.getConnectionType().getValue() == EnumConnectionType.ActiveSocket.getValue()) {
				sb.append(":"); sb.append(basicSocket.getPort()); 
			}
			sb.append(", c."); sb.append(basicSocket.getId()); 
			sb.append("] ");
			
		}

		return sb.toString();

	}
	
	public void verbose(Session session, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= super.VERBOSE.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(session));
			sb.append(format);

			trace(sb.toString(), args);
		}

	}

	public void trace(Session session, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.TRACE.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(session));
			sb.append(format);

			trace(sb.toString(), args);
		}

	}
	
	public void debug(Session session, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.DEBUG.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(session));
			sb.append(format);

			debug(sb.toString(), args);
		}
		
	}

	public void info(Session session, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.INFO.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(session));
			sb.append(format);

			info(sb.toString(), args);
		}
		
	}
	
	public void warn(Session session, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.WARN.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(session));
			sb.append(format);

			warn(sb.toString(), args);
		}
		
	}
		
	public void error(Session session, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.ERROR.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(session));
			sb.append(format);

			error(sb.toString(), args);
		}
		
	}
	

	public void fatal(Session session, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.FATAL.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(session));
			sb.append(format);

			fatal(sb.toString(), args);
		}
		
	}
	
	
	private String getEnvelopeHead(SSLSession session) {

		StringBuffer sb = new StringBuffer();
		
		if (session != null && session.getBasicSocket() != null) {
		
			SSLBasicSocket basicSocket = session.getBasicSocket();
			
			if (basicSocket.getPrefix4Log() != null) {

				sb.append("["); sb.append(basicSocket.getPrefix4Log()); 
				sb.append("] ");

			} 
			
			sb.append("[h."); sb.append(basicSocket.getHostaddress());
			if (basicSocket.getConnectionType() != null && basicSocket.getConnectionType().getValue() == EnumConnectionType.ActiveSocket.getValue()) {
				sb.append(":"); sb.append(basicSocket.getPort()); 
			}
			sb.append(", c."); sb.append(basicSocket.getId()); 
			sb.append("] ");
			
		}

		return sb.toString();

	}	
	
	public void trace(SSLSession session, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.TRACE.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(session));
			sb.append(format);

			trace(sb.toString(), args);
		}

	}
	
	public void debug(SSLSession session, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.DEBUG.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(session));
			sb.append(format);

			debug(sb.toString(), args);
		}
		
	}

	public void info(SSLSession session, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.INFO.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(session));
			sb.append(format);

			info(sb.toString(), args);
		}
		
	}
	
	public void warn(SSLSession session, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.WARN.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(session));
			sb.append(format);

			warn(sb.toString(), args);
		}
		
	}
		
	public void error(SSLSession session, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.ERROR.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(session));
			sb.append(format);

			error(sb.toString(), args);
		}
		
	}
	

	public void fatal(SSLSession session, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.FATAL.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(session));
			sb.append(format);

			fatal(sb.toString(), args);
		}
		
	}
	
	private String getEnvelopeHead(IPAddress address) {

		StringBuffer sb = new StringBuffer();
		
		if (address != null && address.getHostaddress() != null && address.getPort() > 0) {
		
			sb.append("[h."); sb.append(address.getHostaddress()); 
			sb.append(":"); sb.append(address.getPort()); 
			sb.append("] ");
			
		}

		return sb.toString();

	}

	public void trace(IPAddress address, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.TRACE.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(address));
			sb.append(format);

			trace(sb.toString(), args);
		}

	}
	
	public void debug(IPAddress address, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.DEBUG.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(address));
			sb.append(format);

			debug(sb.toString(), args);
		}
		
	}

	public void info(IPAddress address, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.INFO.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(address));
			sb.append(format);

			info(sb.toString(), args);
		}
		
	}
	
	public void warn(IPAddress address, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.WARN.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(address));
			sb.append(format);

			warn(sb.toString(), args);
		}
		
	}
		
	public void error(IPAddress address, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.ERROR.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(address));
			sb.append(format);

			error(sb.toString(), args);
		}
		
	}
	

	public void fatal(IPAddress address, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.FATAL.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(address));
			sb.append(format);

			fatal(sb.toString(), args);
		}
		
	}		
	
	private String getEnvelopeHead(IPAddress address, long connectionId) {

		StringBuffer sb = new StringBuffer();
		
		if (address != null && address.getHostaddress() != null && address.getPort() > 0) {
		
			sb.append("[h."); sb.append(address.getHostaddress()); 
			sb.append(":"); sb.append(address.getPort()); 
			sb.append(", c."); sb.append(connectionId); 
			sb.append("] ");
			
		}

		return sb.toString();

	}
	
	public void trace(IPAddress address, long connectionId, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.TRACE.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(address, connectionId));
			sb.append(format);

			trace(sb.toString(), args);
		}

	}
	
	public void debug(IPAddress address, long connectionId, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.DEBUG.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(address, connectionId));
			sb.append(format);

			debug(sb.toString(), args);
		}
		
	}

	public void info(IPAddress address, long connectionId, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.INFO.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(address, connectionId));
			sb.append(format);

			info(sb.toString(), args);
		}
		
	}
	
	public void warn(IPAddress address, long connectionId, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.WARN.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(address, connectionId));
			sb.append(format);

			warn(sb.toString(), args);
		}
		
	}
		
	public void error(IPAddress address, long connectionId, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.ERROR.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(address, connectionId));
			sb.append(format);

			error(sb.toString(), args);
		}
		
	}
	

	public void fatal(IPAddress address, long connectionId, String format, Object... args) {

		StringBuffer sb = null;
		
		if (logger.getLevel().intLevel() >= Level.FATAL.intLevel()) {

			sb = new StringBuffer();
			sb.append(getEnvelopeHead(address, connectionId));
			sb.append(format);

			fatal(sb.toString(), args);
		}
		
	}	
}



