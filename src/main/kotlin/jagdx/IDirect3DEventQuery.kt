/* IDirect3DEventQuery - Decompiled by JODE
 * Visit http://jode.sourceforge.net/
 */
package jagdx

/**
 * Represents an interface for event queries in Direct3D.
 */
class IDirect3DEventQuery : IUnknown() {

    /**
     * Creates a new instance of IDirect3DEventQuery.
     *
     * @throws Throwable
     */
    init {
        throw Error()
    }

    companion object {
        /**
         * Issues a command to the Direct3D device to insert a query event into the command buffer.
         *
         * @param eventQuery The event query to insert into the command buffer.
         * @return An HRESULT code indicating whether the operation succeeded or failed.
         */
        @JvmStatic
        external fun Issue(eventQuery: Long): Int

        /**
         * Checks whether the query event has been signaled.
         *
         * @param eventQuery The event query to check.
         * @return An HRESULT code indicating whether the operation succeeded or failed. If the query event has been signaled, S_OK is returned; otherwise, S_FALSE is returned.
         */
        @JvmStatic
        external fun IsSignaled(eventQuery: Long): Int
    }
}
