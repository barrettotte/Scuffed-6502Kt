package scuffed6502

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// Test Emulator for each instruction

class EmulatorTests {
    private val cpu: Cpu = Cpu()

    @BeforeEach
    fun setup(){
        cpu.reset()
    }

    @Test
    fun test_basic() {
        // TODO Run basic test case
    }
}
