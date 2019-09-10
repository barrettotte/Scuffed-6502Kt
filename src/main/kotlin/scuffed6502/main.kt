package scuffed6502


fun main(args: Array<String>){

    val bus = Bus()
    val cpu = Cpu() // 6502
    val ram = Ram() // 64Kb


    bus.connectDevice(cpu)
    bus.connectDevice(ram)


    bus.disconnectDevice(cpu)
    bus.disconnectDevice(ram)

}