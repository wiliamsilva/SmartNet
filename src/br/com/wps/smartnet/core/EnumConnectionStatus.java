package br.com.wps.smartnet.core;

import java.util.HashMap;
import java.util.Map;

public enum EnumConnectionStatus {

	Stopped(1)
	{
		@Override
		public String toString() {
			return "Stopped work";
		}
	}
	, StartedWithNoWork(2)
	{
		@Override
		public String toString() {
			return "Started with no work";
		}
	}
	, StartedFull(3)
	{
		@Override
		public String toString() {
			return "Started at working";
		}
	}
	, ReadyToRemove(4)
	{
		@Override
		public String toString() {
			return "Ready to remove";
		}
	}
	, Done(5)
	{
		@Override
		public String toString() {
			return "Done";
		}
	};

    private static final Map<Integer, EnumConnectionStatus> typesByValue = new HashMap<Integer, EnumConnectionStatus>();

    static {
        for (EnumConnectionStatus type : EnumConnectionStatus.values()) {
            typesByValue.put(type.id, type);
        }
    }
    
	private final int id;
	
	EnumConnectionStatus(int id) {this.id = id;}
	
	public int getValue() {return id;}
	
    public static EnumConnectionStatus forValue(int value) {
    	EnumConnectionStatus result = typesByValue.get(value);
        return result;
    }	
	
	
}
