package com.jagex.game.runetek5.buffer;

public interface ArrayBufferGL {

	void putArray(int arraySize, byte[] buffer, int currentPosition);

	int getStride();

	int getId();
	
	long getBufferAddress();
}
