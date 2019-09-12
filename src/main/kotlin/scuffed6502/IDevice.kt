package scuffed6502

@ExperimentalUnsignedTypes
interface IDevice {

    fun write(addr: UShort, data: UByte): Boolean

    fun read(addr: UShort): UByte

    fun connectBus(bus: Bus)

    fun disconnectBus()

}
