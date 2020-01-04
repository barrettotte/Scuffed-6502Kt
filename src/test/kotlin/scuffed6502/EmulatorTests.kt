package scuffed6502

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

// Test Emulator for each instruction

class EmulatorTests {
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
        val pgm = listOf(
                0xA2, 0x0A, 0x8E, 0x00, 0x00, 0xA2, 0x03, 0x8E, 0x01, 0x00, 0xAC, 0x00, 0x00, 0xA9, 0x00, 0x18,
                0x6D, 0x01, 0x00, 0x88, 0xD0, 0xFA, 0x8D, 0x02, 0x00, 0xEA, 0xEA, 0xEA)
        cpu.loadProgram(pgm, 0x8000)

        cpu.reset()
        cpu.step() // process CPU reset (8 cycles)

        cpu.step() // LDX #$0A
        assertEquals(0x0A, cpu.regX)

        cpu.step() // STX $0000
        assertEquals(0x0A, cpu.memory[0x0000])

        cpu.step() // LDX #$03
        assertEquals(0x03, cpu.regX)

        cpu.step() // STX $0001
        assertEquals(0x03, cpu.memory[0x0001])

        cpu.step() // LDY $0000
        assertEquals(0x0A, cpu.regY)

        cpu.step() // LDA #$00
        assertEquals(0x00, cpu.regA)
        assertEquals(1, cpu.getFlag("Z"))

        cpu.step() // CLC
        assertEquals(0, cpu.getFlag("C"))

        // loop  (calculating 10 * 3)
        for(i in 1..10) {
            cpu.step() // ADC $0001
            assertEquals(0x03 * i, cpu.regA)
            cpu.step() // DEY
            assertEquals(0x0A - i, cpu.regY)
            cpu.step() // BNE loop
        }
        assertEquals(0x1E, cpu.regA)
        cpu.step() // STA $0002
        assertEquals(0x1E, cpu.memory[0x0002])

        cpu.step() // NOP
        cpu.step() // NOP
        cpu.step() // NOP

        cpu.logMemory(0x8000)
    }
}
