/* Ping - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.ping;

public class Ping {
	private Ping() throws Throwable {
		throw new Error();
	}

	public static native boolean init();

	public static int ping(byte rawIp0, byte rawIp1, byte rawIp2, byte rawIp3, long timeout) throws Throwable {
		int response = ping0(rawIp0, rawIp1, rawIp2, rawIp3, timeout);
		if (response < 0)
			throw new Exception(String.valueOf(response));
		return response;
	}

	private static native int ping0(byte rawIp0, byte rawIp1, byte rawIp2, byte rawIp3, long timeout);

	public static native void quit();
}
