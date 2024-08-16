/* SimplePeer - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagtheora.misc;

public abstract class SimplePeer {
    static {
        init();
    }

    public long peer;

    protected SimplePeer() {
        /* empty */
    }

    private static native void init();

    public boolean peerPresent() {
        return this.peer == 0L;
    }

    public void setPeer(long peer) {
        this.peer = peer;
    }

    public void resetPeer() {
        if (!peerPresent()) {
            clear();
        }
    }

    protected abstract void clear();

    @SuppressWarnings("removal")
	@Override
    protected void finalize() throws Throwable {
        if (!peerPresent()) {
            resetPeer();
        }
        super.finalize();
    }

    protected abstract void q();

    protected abstract void f();

    protected abstract void m();
}
