package com.jagex.game.runetek5.buffer;

/**
 * 
 * @author Steven - 13/09/2023
 *
 */
public interface ElementsBufferGL {

	long getBufferAddress();

	void putElements(byte[] data, int size);
	
	int getId();

}
