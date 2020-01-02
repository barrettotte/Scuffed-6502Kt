package scuffed6502

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

// Test Disassembler for each instruction

class DissassemblerTests {
    private val cpu: Cpu = Cpu()

    @BeforeEach
    fun setup(){
        cpu.reset()
    }

    @Test
    fun test_basic() {
        // A2 0A 8E 00 00 A2 03 8E 01 00 AC 00 00 A9 00 18 6D 01 00 88 D0 FA 8D 02 00 EA EA EA
        /*  assembled at https://www.masswerk.at/6502/assembler.html
          *=$8000
		    LDX #$0A
			STX $0000
			LDX #$03
			STX $0001
 			LDY $0000
			LDA #$00
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
        val entry = 0x600
        val pgm = listOf(
                0xA2, 0x0A, 0x8E, 0x00, 0x00, 0xA2, 0x03, 0x8E, 0x01, 0x00, 0xAC, 0x00, 0x00, 0xA9, 0x00, 0x18,
                0x6D, 0x01, 0x00, 0x88, 0xD0, 0xFA, 0x8D, 0x02, 0x00, 0xEA, 0xEA, 0xEA)
        val da = cpu.loadProgram(pgm, entry)
        outputDisassembly("./disassembly.txt", da, entry - 4, entry + 32)

        assertEquals(32768, da.size)

        // Test hex dumps
        assertEquals(da[0x0600]?.hex, "A2 0A")
        assertEquals(da[0x0602]?.hex, "8E 00 00")
        assertEquals(da[0x0605]?.hex, "A2 03")
        assertEquals(da[0x0607]?.hex, "8E 01 00")
        assertEquals(da[0x060A]?.hex, "AC 00 00")
        assertEquals(da[0x060D]?.hex, "A9 00")
        assertEquals(da[0x060F]?.hex, "18")
        assertEquals(da[0x0610]?.hex, "6D 01 00")
        assertEquals(da[0x0613]?.hex, "88")
        assertEquals(da[0x0614]?.hex, "D0 FA")
        assertEquals(da[0x0616]?.hex, "8D 02 00")
        assertEquals(da[0x0619]?.hex, "EA")
        assertEquals(da[0x061A]?.hex, "EA")
        assertEquals(da[0x061B]?.hex, "EA")
    }

    private fun outputDisassembly(path: String, memory: Map<Int, Disassembly>, start: Int = 0, end: Int = 1024){
        File(path).printWriter().use { out ->
            out.println("-".repeat(90))
            out.println(" Index    Address               Assembly           Mode    Opcode  Cycles      Hex Dump")
            out.println("-".repeat(90))
            memory.forEach{(k,v) ->
                if(k in start..end){
                    out.println("[${k.toString().padStart(5, '0')}]    $v ")
                }
            }
        }
    }

}