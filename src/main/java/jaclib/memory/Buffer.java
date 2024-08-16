/* Buffer - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.memory;

public interface Buffer {
	int getSize();

	long getBufferAddress();

	void put(byte[] data, int i, int i0, int size);
}
