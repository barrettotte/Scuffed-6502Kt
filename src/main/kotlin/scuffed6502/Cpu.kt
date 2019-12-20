package scuffed6502

import com.opencsv.CSVReaderBuilder
import javafx.scene.input.Mnemonic
import java.io.BufferedReader
import java.io.InputStreamReader

class Cpu {

    companion object {
        private const val INSTRUCTION_FILE = "instructions.csv" // /resources/instructions.csv
        private const val MEMSIZE   = 0x10000                   // 65536; ram, audio, graphics, IO sections
        private const val SP_ENTRY  = 0x100                     // Entry point for stack
        private const val PGM_ENTRY = 0x8000                    // program entry, can be modified with RST_VX

        // Vectors
        private const val RST_VL = 0xFFFC.toShort()
        private const val RST_VH = 0xFFFD.toShort()
        private const val NMI_VL = 0xFFFA.toShort()
        private const val NMI_VH = 0xFFFB.toShort()
        private const val INT_VL = 0xFFFE.toShort()
        private const val INT_VH = 0xFFFF.toShort()
    }

    private var memory: IntArray = IntArray(MEMSIZE)

    private val instructions: List<Instruction> = initInstructionSet(INSTRUCTION_FILE)

    // Registers
    private var regPC: Int = 0          // program counter
    private var regSP: Int = SP_ENTRY   // stack pointer
    private var regA:  Int = 0          // accumulator
    private var regX:  Int = 0          // register x
    private var regY:  Int = 0          // register y

    // Flags
    private var flagC = 0               // carry flag
    private var flagZ = 0               // zero flag
    private var flagI = 0               // interrupt disable flag
    private var flagD = 0               // decimal mode flag
    private var flagB = 0               // break command flag
    private var flagU = 0               // unused flag
    private var flagV = 0               // overflow flag
    private var flagN = 0               // negative flag


    fun reset(){
        regSP = SP_ENTRY
        memory = IntArray(MEMSIZE)
        regA = 0; regX = 0; regY = 0; regPC = 0
        setStatus(0)
    }

    fun getStatus() = (flagC shl 0) or (flagZ shl 1) or (flagI shl 2) or (flagD shl 3) or
                      (flagB shl 4) or (flagU shl 5) or (flagV shl 6) or (flagN shl 7) or 0

    fun setStatus(status: Int){
        flagC = 1 and (status shr 0); flagZ = 1 and (status shr 1)
        flagI = 1 and (status shr 2); flagD = 1 and (status shr 3)
        flagB = 1 and (status shr 4); flagU = 1 and (status shr 5)
        flagV = 1 and (status shr 6); flagN = 1 and (status shr 7)
    }

    fun read(addr: Int): Int{
        return memory[addr % MEMSIZE]
    }

    fun write(addr: Int, data: Int){
        memory[addr % MEMSIZE] = data
    }

    fun interrupt(){
        println("Interrupt!")
    }

    fun signal(){
        println("Signal!")
    }

    fun step(){
        val opcode = loadInstruction() and 0xFF
        opcodeHandler(opcode)
    }

    private fun opcodeHandler(opcode: Int){
        when(opcode){
            0x00 -> opBRK()
        }
    }

    fun getInstructionSet() = instructions
    private fun loadInstruction() = memory[regPC++]
    private fun opcodeToMnemonic(opcode: Int) = instructions.find{it.opcode == opcode}?.mnemonic ?: "???"
    private fun opBRK() = println("BRK")


    private fun initInstructionSet(fileName: String): List<Instruction>{
        val instructs : MutableList<Instruction> = mutableListOf()
        val fileReader = BufferedReader(InputStreamReader(this.javaClass.getResourceAsStream("/$fileName")))
        val csvReader = CSVReaderBuilder(fileReader).withSkipLines(1).build()
        try{
            csvReader.readAll().forEach{record ->
                instructs.add(Instruction(record[0], record[1].toInt(16),
                    record[2].toInt(), record[3].toInt(), record[4]))
            }
        }catch(e: Exception){
            println("Error loading instructions.")
            e.printStackTrace()
        }finally{
            fileReader.close()
            csvReader!!.close()
        }
        return instructs
    }

}

