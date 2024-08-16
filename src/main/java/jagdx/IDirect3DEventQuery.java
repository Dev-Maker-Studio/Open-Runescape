/* IDirect3DEventQuery - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx;

/**
 * Represents an interface for event queries in Direct3D.
 */
public class IDirect3DEventQuery extends IUnknown {
    
    /**
     * Creates a new instance of IDirect3DEventQuery.
     *
     * @throws Throwable
     */
    public IDirect3DEventQuery() throws Throwable {
        throw new Error();
    }

    /**
     * Issues a command to the Direct3D device to insert a query event into the command buffer.
     *
     * @param eventQuery The event query to insert into the command buffer.
     * @return An HRESULT code indicating whether the operation succeeded or failed.
     */
    public static native int Issue(long eventQuery);

    /**
     * Checks whether the query event has been signaled.
     *
     * @param eventQuery The event query to check.
     * @return An HRESULT code indicating whether the operation succeeded or failed. If the query event has been signaled, S_OK is returned; otherwise, S_FALSE is returned.
     */
    public static native int IsSignaled(long eventQuery);
}
