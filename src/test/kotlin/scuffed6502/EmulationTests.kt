package scuffed6502

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

@ExperimentalUnsignedTypes
class BasicTest {

    private val bus = Bus()
    private val cpu = Cpu(bus)
    private val ram = Ram(bus)

    @BeforeEach
    fun setup(){
        bus.connectDevice(cpu)
        bus.connectDevice(ram)
    }

    @AfterEach
    fun teardown(){
        cpu.connectBus(bus)
        ram.connectBus(bus)
    }

    @Test
    fun test_emulation_setup(){
        assertEquals(bus.devices.size, 2)
        assertEquals(cpu.instructions.size, 256)

        assertFalse(cpu.flags["C"]!!)
        assertFalse(cpu.flags["Z"]!!)
        assertFalse(cpu.flags["I"]!!)
        assertFalse(cpu.flags["D"]!!)
        assertFalse(cpu.flags["B"]!!)
        assertFalse(cpu.flags["U"]!!)
        assertFalse(cpu.flags["V"]!!)
        assertFalse(cpu.flags["N"]!!)
    }

    @Test
    fun test_flagset(){
        assertFalse(cpu.getFlag("C"))
        cpu.setFlag("C", true)
        cpu.setFlag("U", true)
        assertTrue(cpu.getFlag("C"))
        assertFalse(cpu.getFlag("Z"))
        assertTrue(cpu.getFlag("U"))
        assertEquals(cpu.regStatus, 132U.toUByte())
    }


    @Test
    fun test_disassemble(){
        /* assembled at https://www.masswerk.at/6502/assembler.html
            *=$8000
			LDX #10
			STX $0000
			LDX #3
			STX $0001
			LDY $0000
			LDA #0
			CLC
			loop
			ADC $0001
			DEY
			BNE loop
			STA $0002
			NOP
			NOP
			NOP
        */
        var idx = 0
        val objCode = listOf<UByte>(
                0xA2U, 0x0AU, 0x8EU, 0x00U, 0x00U, 0xA2U, 0x03U, 0x8EU, 0x01U, 0x00U, 0xACU, 0x00U, 0x00U, 0xA9U, 0x00U, 0x18U,
                0x6DU, 0x01U, 0x00U, 0x88U, 0xD0U, 0xFAU, 0x8DU, 0x02U, 0x00U, 0xEAU, 0xEAU, 0xEAU
        )
        val offset = 0x8000.toUShort()
        objCode.forEach { _ ->
            ram.write((offset+idx.toUInt()).toUShort(), objCode[idx++])
        }
        ram.write(0xFFFCU, 0x00U)
        ram.write(0xFFFDU, 0x80U)

        assertEquals(bus, cpu.bus)
        assertEquals(cpu.read(0xFFFCU), 0x00U.toUByte())
        assertEquals(cpu.read(0xFFFDU), 0x80U.toUByte())
        assertEquals(cpu.read(0x8000U), 0xA2U.toUByte())

        val memory = cpu.disassemble(0x0000U, 0xFFFFU)
        assertNotNull(memory)
        writeMemory("./disassembly-01.txt", memory)

        assertEquals("\$8000: LDX #\$0a {IMM}", memory[32768U])
        cpu.reset()
    }

    @Test
    fun test_runProgram(){
        var objIdx = 0
        val objCode = listOf<UByte>(
                0xA2U, 0x0AU, 0x8EU, 0x00U, 0x00U, 0xA2U, 0x03U, 0x8EU, 0x01U, 0x00U, 0xACU, 0x00U, 0x00U, 0xA9U, 0x00U, 0x18U,
                0x6DU, 0x01U, 0x00U, 0x88U, 0xD0U, 0xFAU, 0x8DU, 0x02U, 0x00U, 0xEAU, 0xEAU, 0xEAU
        )
        val offset = 0x8000.toUShort()
        objCode.forEach { _ ->
            ram.write((offset+objIdx.toUInt()).toUShort(), objCode[objIdx++])
        }
        ram.write(0xFFFCU, 0x00U)
        ram.write(0xFFFDU, 0x80U)

        var idx = 0x0000U
        while(idx < 0xFFFFU){
            do {
                cpu.tickClock()
            }
            while(!cpu.complete())
            idx++
        }
        File("./result.txt").printWriter().use{ out ->
            cpu.logData.forEach{ entry ->
                out.println(entry.toString())
            }
        }
        cpu.reset()
    }

    private fun writeMemory(path: String, memory: Map<UShort, String>){
        File(path).printWriter().use {out ->
            memory.forEach { (k, v) ->
                out.println(v)
            }
        }
    }



}