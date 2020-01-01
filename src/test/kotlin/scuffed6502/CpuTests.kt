package scuffed6502

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

// General tests

class CpuTests {
    private val cpu: Cpu = Cpu()

    @BeforeEach
    fun setup(){
        cpu.reset()
    }

    @AfterEach
    fun teardown(){
        //println("\ndone.")
    }

    @Test
    fun test_cpu_init(){
        assertEquals(151, cpu.getInstructionSet().size)
        assertEquals(32, cpu.getStatus()) // U flag active still 00100000
        assertEquals(65536, cpu.getMemory().size)
    }

    @Test
    fun test_status(){
        cpu.setStatus(2)
        assertEquals(2, cpu.getStatus())
        cpu.setStatus(123) // 0111 1011 -> NVUB DIZC
        assertEquals(123, cpu.getStatus())
        assertEquals(cpu.getFlag("V"), 1)
    }

    @Test
    fun test_disassemble() {
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
        val opcodes = listOf<Int>(0xA2, 0x0A, 0x8E, 0x00, 0x00, 0xA2, 0x03, 0x8E, 0x01, 0x00, 0xAC, 0x00, 0x00,
                0xA9, 0x00, 0x18, 0x6D, 0x01, 0x00, 0x88, 0xD0, 0xFA, 0x8D, 0x02, 0x00, 0xEA, 0xEA, 0xEA)
        val entry = 0x8000
        cpu.loadProgram(opcodes, entry)
        val disassembled = cpu.disassemble(0x0000, 0xFFFF)
        outputDisassembly("./disassembly.txt", disassembled)
    }

    private fun outputDisassembly(path: String, memory: Map<Int, String>){
        File(path).printWriter().use { out ->
            out.println("------------------------------------------------------------------------------")
            out.println(" Index    Address            Assembly             Mode    Opcode  Cycles  Size")
            out.println("------------------------------------------------------------------------------")
            memory.forEach{
                (k,v) -> out.println("[${k.toString().padStart(5,'0')}]    $v")
            }
        }
    }

}