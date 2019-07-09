package br.com.wps.smartnet.core;

import java.util.HashMap;
import java.util.Map;

public enum EnumDisconnectionReason {

	FirstMessageTimeout(1)
	{
		@Override
		public String toString() {
			return "The timeout of first message was reached";
		}
	}
	, Unauthorized(2)
	{
		@Override
		public String toString() {
			return "Connection not authorized";
		}
	}
	, IdleTimeout(3)
	{
		@Override
		public String toString() {
			return "The idle timeout was reached";
		}
	}
	, ProcessTimeout(4)
	{
		@Override
		public String toString() {
			return "The process timeout was reached";
		}
	}
	, HostUnplugged(5)
	{
		@Override
		public String toString() {
			return "Host disconnected";
		}
	}
	, ReceiveError(6)
	{
		@Override
		public String toString() {
			return "Error during bytes receiving";
		}
	}
	, SendError(7)
	{
		@Override
		public String toString() {
			return "Error during bytes sending";
		}
	}
	, ProcessError(8)
	{
		@Override
		public String toString() {
			return "Error during message process";
		}
	}
	, ServerStopRequest(9)
	{
		@Override
		public String toString() {
			return "Server stop was request";
		}
	}
	, Unknown(10)
	{
		@Override
		public String toString() {
			return "Unknown or developer did not set this";
		}
	}
	, ReconnectTimeout(11)
	{
		@Override
		public String toString() {
			return "The timeout for reconnect was reached";
		}
	}
	, InvalidMessage(12)
	{
		@Override
		public String toString() {
			return "Invalid message catched on framing";
		}
	}
	, MessagePreparationError(13)
	{
		@Override
		public String toString() {
			return "Error during message preparation";
		}
	}
	, ReachedMaximumConnection(14)
	{
		@Override
		public String toString() {
			return "Reached maximum of connections";
		}
	}
	, AcceptError(15)
	{
		@Override
		public String toString() {
			return "Error during acceptance of socket";
		}
	}
	, ConnectionError(16)
	{
		@Override
		public String toString() {
			return "Error during connetion of socket";
		}
	}
	, ExitRequest(17)
	{
		@Override
		public String toString() {
			return "Exit request";
		}
	}
	;

    private static final Map<Integer, EnumDisconnectionReason> typesByValue = new HashMap<Integer, EnumDisconnectionReason>();

    static {
        for (EnumDisconnectionReason type : EnumDisconnectionReason.values()) {
            typesByValue.put(type.id, type);
        }
    }
    
	private final int id;
	
	EnumDisconnectionReason(int id) {this.id = id;}
	
	public int getValue() {return id;}
	
    public static EnumDisconnectionReason forValue(int value) {
    	EnumDisconnectionReason result = typesByValue.get(value);
        return result;
    }	
	
	
}
