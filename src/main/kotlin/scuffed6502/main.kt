package scuffed6502


@ExperimentalUnsignedTypes
fun main(args: Array<String>){

    val bus = Bus()
    val cpu = Cpu(bus) // 6502
    val ram = Ram(bus) // 64Kb

    //bus.connectDevice(cpu)
    bus.connectDevice(ram)


    //bus.disconnectDevice(cpu)
    bus.disconnectDevice(ram)
}
