package scuffed6502

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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
        assertEquals(0, cpu.getStatus())
    }

    @Test
    fun test_status(){
        cpu.setStatus(2)
        assertEquals(2, cpu.getStatus())
        cpu.setStatus(123)
        assertEquals(123, cpu.getStatus())
    }

}