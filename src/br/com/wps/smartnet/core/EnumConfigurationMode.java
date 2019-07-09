package br.com.wps.smartnet.core;

import java.util.HashMap;
import java.util.Map;

public enum EnumConfigurationMode {

	Client(1)
	{
		@Override
		public String toString() {
			return "Client mode";
		}
	}
	, Server(2)
	{
		@Override
		public String toString() {
			return "Server mode";
		}
	};

    private static final Map<Integer, EnumConfigurationMode> typesByValue = new HashMap<Integer, EnumConfigurationMode>();

    static {
        for (EnumConfigurationMode type : EnumConfigurationMode.values()) {
            typesByValue.put(type.id, type);
        }
    }
    
	private final int id;
	
	EnumConfigurationMode(int id) {this.id = id;}
	
	public int getValue() {return id;}
	
    public static EnumConfigurationMode forValue(int value) {
    	EnumConfigurationMode result = typesByValue.get(value);
        return result;
    }	
	
	
}
