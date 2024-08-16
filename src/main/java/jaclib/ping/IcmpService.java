/* IcmpService - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jaclib.ping;

public abstract class IcmpService implements Runnable {
	protected IcmpService() {
		/* empty */
	}

	protected abstract void notify(int i);

	protected abstract void notify(int i, int i0, int i1);

	@Override
	public native void run();

	public native void quit();

	public static native boolean available();

	public native void m();

	public native void h();

	protected abstract void method11597(int i);

	protected abstract void method11598(int i);

	protected abstract void method11599(int i, int i2, int i3);

	protected abstract void method11600(int i, int i4, int i5);
}
