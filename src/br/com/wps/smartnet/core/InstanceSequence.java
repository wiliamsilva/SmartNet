package br.com.wps.smartnet.core;

public class InstanceSequence {

	private static long value;

	static {
		value = 0L;
	}

	public static long nextValue() {

		if (value == Long.MAX_VALUE) {
			value = 0L;
		}

		value++;

		return value;

	}
	
	
}
