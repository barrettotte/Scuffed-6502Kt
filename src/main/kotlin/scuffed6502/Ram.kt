package scuffed6502

@ExperimentalUnsignedTypes
class Ram(private val storage: UByteArray = UByteArray(65536) {0U}) : IDevice {

    private var bus: Bus? = null

    override fun connectBus(bus: Bus){
        bus.connectDevice(this)
        this.bus = bus
    }

    override fun disconnectBus() {
        bus?.disconnectDevice(this)
        this.bus = null
    }

    override fun write(addr: UShort, data: UByte): Boolean {
        if(addr > this.storage.size.toUInt()){
            return false
        }
        this.storage[addr.toInt()] = data
        return true
    }

    override fun read(addr: UShort): UByte = this.storage[addr.toInt()]
}
