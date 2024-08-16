package com.jagex.game.runetek5.encoding;

import com.jagex.game.runetek5.buffer.Packet;
import com.jagex.game.runetek5.utils.Utils;

/**
 * 
 * @author Brainpower - 19/04/2022
 *
 */
public class HuffmanList {

	public static Huffman HUFFMAN;

	public static void initialize(Huffman huffman) {
	    HuffmanList.HUFFMAN = huffman;
	}
	
	public static String decode(Packet buffer) {
	    return HuffmanList.decodeHuffmanMessage(buffer);
	}

	public static String decodeHuffmanMessage(Packet buffer) {
	    try {
	        int size = buffer.readUnsignedSmart();
	        if (size > 32767) {
	            size = 32767;
	        }
	        byte[] data = new byte[size];
	        buffer.currentPosition += HuffmanList.HUFFMAN.calculateDecompressedLength(buffer.buffer, buffer.currentPosition, data, 0, size);
	        return Utils.readString(data, 0, size);
	    } catch (Exception exception) {
	        return "Cabbage";
	    }
	}
}