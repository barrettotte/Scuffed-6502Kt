package scuffed6502

@ExperimentalUnsignedTypes
class Ram(val bus: Bus, private val storage: UByteArray = UByteArray(65536) {0U}) : IDevice {

    override fun connectBus(bus: Bus){
        bus.connectDevice(this)
    }

    override fun disconnectBus() {
        bus?.disconnectDevice(this)
    }

    override fun write(addr: UShort, data: UByte): Boolean {
        if(addr > this.storage.size.toUInt()){
            return false
        }
        this.storage[addr.toInt()] = data
        return true
    }

    override fun read(addr: UShort): UByte = this.storage[addr.toInt()]

    fun dump(){
        var idx = 0
        storage.forEach { b -> println("%04x: ".format(idx++).toUpperCase() + "%02x".format(b.toInt()).toUpperCase()) }
    }

    fun reset(){
        storage.forEachIndexed { index, uByte ->
            storage[index] = 0U
        }
    }
}
