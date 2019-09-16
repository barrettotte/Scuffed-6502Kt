package scuffed6502

@ExperimentalUnsignedTypes
class Bus(val devices: MutableList<IDevice> = mutableListOf()) {

    fun connectDevice(device: IDevice){
        this.devices.add(device)
    }

    fun disconnectDevice(device: IDevice){
        this.devices.remove(device)
    }

    fun readFromRAM(addr: UShort) = getRAM().read(addr)

    fun writeToRAM(addr: UShort, data: UByte): Boolean {
        val ram = getRAM()
        ram.write(addr, data)
        return true
    }

    private fun getRAM() = this.devices.filterIsInstance<Ram>().first()

}