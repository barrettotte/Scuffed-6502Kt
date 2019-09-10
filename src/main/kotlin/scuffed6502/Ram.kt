package scuffed6502

import java.lang.Exception

class Ram(private val storage: UByteArray = UByteArray(65536) {0U}) : IDevice {

    private lateinit var bus: Bus

    override fun connectToBus(bus: Bus){
        this.bus = bus
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
