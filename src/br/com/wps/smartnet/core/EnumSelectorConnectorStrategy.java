package br.com.wps.smartnet.core;

import java.util.HashMap;
import java.util.Map;

public enum EnumSelectorConnectorStrategy {

	Default(1)
	{
		@Override
		public String toString() {
			return "Default (use first available connector)";
		}
	},
	BalanceConnector(2)
	{
		@Override
		public String toString() {
			return "Balance connectors (serial and recicle use connector)";
		}
	};

    private static final Map<Integer, EnumSelectorConnectorStrategy> typesByValue = new HashMap<Integer, EnumSelectorConnectorStrategy>();

    static {
        for (EnumSelectorConnectorStrategy type : EnumSelectorConnectorStrategy.values()) {
            typesByValue.put(type.id, type);
        }
    }
    
	private final int id;
	
	EnumSelectorConnectorStrategy(int id) {this.id = id;}
	
	public int getValue() {return id;}
	
    public static EnumSelectorConnectorStrategy forValue(int value) {
    	EnumSelectorConnectorStrategy result = typesByValue.get(value);
        return result;
    }	
	
	
}
