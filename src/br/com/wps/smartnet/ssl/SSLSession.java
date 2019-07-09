package br.com.wps.smartnet.ssl;

import java.util.HashMap;

import br.com.wps.smartnet.core.Configuration;
import br.com.wps.smartnet.logger.SmartLog;
import br.com.wps.smartnetutils.ext.ExceptionUtils;

public class SSLSession implements AutoCloseable, Comparable<SSLSession> {

	private static long createdInstances;

	static {
		createdInstances = 0L;
	}
	
	public static long getCreatedInstantes() {
		return createdInstances;
	}	

	private long id; 
	
	private Configuration refConfig;
	private SSLBasicSocket basicSocket;
	private SSLSender sender;
	private HashMap<String, Object> stateMap;

	public SmartLog logger;

	private SSLSession() {
		
		logger = new SmartLog(this);
		stateMap = new HashMap<String, Object>();
		
	}
	
	public SSLSession(Configuration refConfig, SSLBasicSocket basicSocket, SSLSender sender) {

		this();
		
		if (createdInstances == Long.MAX_VALUE) {
			createdInstances = 0L;
		}
		createdInstances++;
		
		this.id = createdInstances;
		this.refConfig = refConfig;
		this.basicSocket = basicSocket;
		this.sender = sender;
		
		if (sender != null) {
			sender.setRefSession(this);
		}

	}

	public Configuration getRefConfig() {
		return refConfig;
	}

	public SSLBasicSocket getBasicSocket() {
		return basicSocket;
	}

	public SSLSender getSender() {
		return sender;
	}

	@Override
	public synchronized void close() throws Exception {

		try {
			
			if (basicSocket != null) {
				basicSocket.close();
			}

			if (stateMap != null) {
				stateMap.clear();
			}
						
		} catch (Exception e) {
			logger.error(this, "Error while closing session. Error: %s", ExceptionUtils.rootCauseMessage(e));
		} catch (Throwable e) {
			logger.error(this, "Error while closing session. Error: %s", ExceptionUtils.rootCauseMessage(e));
		}		

	}
	
	public void setProperty(String name, String value) {

		if (this.stateMap != null) {
			this.stateMap.put(name, value);
		}
		
	}

	public void setProperty(String name, Long value) {

		if (this.stateMap != null) {
			this.stateMap.put(name, value);
		}
		
	}

	public void setProperty(String name, Integer value) {

		if (this.stateMap != null) {
			this.stateMap.put(name, value);
		}
		
	}

	public void setProperty(String name, Short value) {

		if (this.stateMap != null) {
			this.stateMap.put(name, value);
		}
		
	}

	public void setProperty(String name, Double value) {

		if (this.stateMap != null) {
			this.stateMap.put(name, value);
		}
		
	}

	public void setProperty(String name, Float value) {

		if (this.stateMap != null) {
			this.stateMap.put(name, value);
		}
		
	}

	public void setProperty(String name, Boolean value) {

		if (this.stateMap != null) {
			this.stateMap.put(name, value);
		}
		
	}

	public String getPropertyAsString(String name) {

		String result = null;
		
		if (this.stateMap != null) {
			result = (String) this.stateMap.get(name);
		}
		
		return result;

	}

	public Long getPropertyAsLong(String name) {

		Long result = null;
		Object object = null;
		
		if (this.stateMap != null) {
			
			object = this.stateMap.get(name);
			
			if (object != null) {
				
				if (object instanceof Long) {
					result = (Long) object;
				}
				else if (object instanceof Integer) {
					result = ((Integer) object).longValue();
				}
				else if (object instanceof Short) {
					result = ((Short) object).longValue();
				}

			}

		}
		
		return result;

	}	
	public Integer getPropertyAsInteger(String name) {

		Integer result = null;
		Object object = null;
		
		if (this.stateMap != null) {
			
			object = this.stateMap.get(name);
			
			if (object != null) {
				
				if (object instanceof Long) {
					result = ((Long) object).intValue();
				}
				else if (object instanceof Integer) {
					result = (Integer) object;
				}
				else if (object instanceof Short) {
					result = ((Short) object).intValue();
				}

			}

		}
		
		return result;
		
	}
	
	public Short getPropertyAsShort(String name) {

		Short result = null;
		Object object = null;
		
		if (this.stateMap != null) {
			
			object = this.stateMap.get(name);
			
			if (object != null) {
				
				if (object instanceof Long) {
					result = ((Long) object).shortValue();
				}
				else if (object instanceof Integer) {
					result = ((Integer) object).shortValue();
				}
				else if (object instanceof Short) {
					result = (Short) object;
				}

			}

		}

		return result;
		
	}

	public Double getPropertyAsDouble(String name) {

		Double result = null;
		Object object = null;
		
		if (this.stateMap != null) {
			
			object = this.stateMap.get(name);
			
			if (object != null) {
				
				if (object instanceof Double) {
					result = (Double) object;
				}
				else if (object instanceof Float) {
					result = ((Float) object).doubleValue();
				}

			}

		}

		return result;
		
	}
	public Float getPropertyAsFloat(String name) {

		Float result = null;
		Object object = null;
		
		if (this.stateMap != null) {
			
			object = this.stateMap.get(name);
			
			if (object != null) {
				
				if (object instanceof Double) {
					result = ((Double) object).floatValue();
				}
				else if (object instanceof Float) {
					result = (Float) object;
				}

			}

		}

		return result;
		
	}

	public Boolean getPropertyAsBoolean(String name) {

		Boolean result = null;
		
		if (this.stateMap != null) {
			result = (Boolean) this.stateMap.get(name);
		}
		
		return result;
		
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public int compareTo(SSLSession toComparate) {

		if (this.getId()  < toComparate.getId()) {
			return -1;
		}
		
		if (this.getId()  > toComparate.getId()) {
			return 1;
		}

		return 0;

	}
	
}



