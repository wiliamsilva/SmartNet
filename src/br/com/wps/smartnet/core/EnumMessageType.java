package br.com.wps.smartnet.core;

import java.util.HashMap;
import java.util.Map;

public enum EnumMessageType {

	ReceiveOnly(1)
	{
		@Override
		public String toString() {
			return "Receive only";
		}
	},
	ReceiveSych(2)
	{
		@Override
		public String toString() {
			return "Receive synchrnonized";
		}
	},
	SendOnly(3)
	{
		@Override
		public String toString() {
			return "Send only";
		}
	}
	, SendAndConfirm(4)
	{
		@Override
		public String toString() {
			return "Send and confirm";
		}
	}
	, SendAndReceive(5)
	{
		@Override
		public String toString() {
			return "Send and receive";
		}
	}
	, SendFuture(6)
	{
		@Override
		public String toString() {
			return "Send future";
		}
	}
	, SendToAll(7)
	{
		@Override
		public String toString() {
			return "Send to all";
		}
	}
	;

    private static final Map<Integer, EnumMessageType> typesByValue = new HashMap<Integer, EnumMessageType>();

    static {
        for (EnumMessageType type : EnumMessageType.values()) {
            typesByValue.put(type.id, type);
        }
    }
    
	private final int id;
	
	EnumMessageType(int id) {this.id = id;}
	
	public int getValue() {return id;}
	
    public static EnumMessageType forValue(int value) {
    	EnumMessageType result = typesByValue.get(value);
        return result;
    }	
	
	
}
