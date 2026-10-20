package jaclib.memory

interface Source {
    fun getSize(): Int
    fun getBufferAddress(): Long
}
