package scuffed6502

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

@ExperimentalUnsignedTypes
class BasicTest {

    private val bus = Bus()
    private val cpu = Cpu()
    private val ram = Ram()

    @Test
    fun test_emulation_setup(){
        bus.connectDevice(cpu)
        bus.connectDevice(ram)
        assertEquals(bus.devices.size, 2)

        assertEquals(cpu.instructions.size, 256)

        cpu.tickClock()

        bus.disconnectDevice(cpu)
        bus.disconnectDevice(ram)
        assertEquals(bus.devices.size, 0)
    }



}