package br.com.wps.smartnet.core;

import java.util.HashMap;
import java.util.Map;

public enum EnumConnectionType {

	ActiveSocket(1)
	{
		@Override
		public String toString() {
			return "Active socket";
		}
	}
	, RemoteSocket(2)
	{
		@Override
		public String toString() {
			return "Remote socket";
		}
	};

    private static final Map<Integer, EnumConnectionType> typesByValue = new HashMap<Integer, EnumConnectionType>();

    static {
        for (EnumConnectionType type : EnumConnectionType.values()) {
            typesByValue.put(type.id, type);
        }
    }
    
	private final int id;
	
	EnumConnectionType(int id) {this.id = id;}
	
	public int getValue() {return id;}
	
    public static EnumConnectionType forValue(int value) {
    	EnumConnectionType result = typesByValue.get(value);
        return result;
    }	
	
	
}
