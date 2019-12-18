package scuffed6502

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

@ExperimentalUnsignedTypes
class BasicTest {

    @BeforeEach
    fun setup(){
        out.println("starting");
    }

    @AfterEach
    fun teardown(){
        out.println("done");
    }

    @Test
    fun test_emulation_setup(){
        
    }

}