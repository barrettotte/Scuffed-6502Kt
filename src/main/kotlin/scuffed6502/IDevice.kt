package scuffed6502

interface IDevice {

    fun write(addr: UShort, data: UByte): Boolean

    fun read(addr: UShort): UByte?

    fun connectToBus(bus: Bus)

}
