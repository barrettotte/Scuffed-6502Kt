package scuffed6502

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.lang.Exception

// General CPU tests

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
    fun test_cpuInit(){
        assertEquals(256, cpu.instructions.size)
        assertEquals(32, cpu.getStatus()) // U flag active still 00100000
        assertEquals(65536, cpu.memory.size)
    }

    @Test
    fun test_status(){
        cpu.setStatus(2)
        assertEquals(2, cpu.getStatus())
        cpu.setStatus(123) // 0111 1011 -> NVUB DIZC
        assertEquals(123, cpu.getStatus())
    }

    @Test
    fun test_getFlag(){
        assertEquals(0, cpu.getFlag("N"))
        assertEquals(0, cpu.getFlag("V"))
        assertEquals(1, cpu.getFlag("U"))
        assertEquals(0, cpu.getFlag("B"))
        assertEquals(0, cpu.getFlag("D"))
        assertEquals(0, cpu.getFlag("I"))
        assertEquals(0, cpu.getFlag("Z"))
        assertEquals(0, cpu.getFlag("C"))
        assertThrows<CpuException>{cpu.getFlag("X")}
    }

    @Test
    fun test_wipeMemory(){
        cpu.wipeMemory()
        val comp = IntArray(0x10000)
        cpu.memory.forEachIndexed{idx,byte -> assertEquals(comp[idx], byte)}
    }

}
