package scuffed6502

import com.opencsv.CSVReaderBuilder
import java.io.BufferedReader
import java.io.InputStreamReader

class Cpu {

    companion object {
        private const val INSTRUCTION_FILE = "instructions.csv" // /resources/instructions.csv
        private const val MEMSIZE = 0x10000                     // 65536; ram, audio, graphics, IO sections
        private const val SP_ENTRY = 0xFF                       //
    }

    private var memory: IntArray = IntArray(MEMSIZE)

    val instructions: List<Instruction> = loadInstructions(INSTRUCTION_FILE)

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
        regA  = 0; regX  = 0; regY  = 0; regPC = 0
        flagC = 0; flagZ = 0; flagI = 0; flagD = 0
        flagB = 0; flagU = 0; flagV = 0; flagN = 0
    }

    fun setStatus(status: Int){
        flagC = 1 and (status shr 0)
        flagZ = 1 and (status shr 1)
        flagI = 1 and (status shr 2)
        flagD = 1 and (status shr 3)
        flagB = 1 and (status shr 4)
        flagU = 1 and (status shr 5)
        flagV = 1 and (status shr 6)
        flagN = 1 and (status shr 7)
    }

    fun getStatus(): Int{
        return 0 or (flagC shl 0) or (flagZ shl 1) or (flagI shl 2) or (flagD shl 3) or
                    (flagB shl 4) or (flagU shl 5) or (flagV shl 6) or (flagN shl 7)
    }

    fun read(addr: Int): Int{
        return memory[addr % MEMSIZE]
    }

    fun write(addr: Int, data: Int){
        memory[addr % MEMSIZE] = data
    }

    private fun peek(): Int{
        regSP = (regSP + 1) and 0xFF
        return read(0x100 or regSP)
    }

    private fun loadInstructions(fileName: String): List<Instruction>{
        val instructs : MutableList<Instruction> = mutableListOf()
        val fileReader = BufferedReader(InputStreamReader(this.javaClass.getResourceAsStream("/$fileName")))
        val csvReader = CSVReaderBuilder(fileReader).withSkipLines(1).build()
        try{
            for(record in csvReader.readAll()){
                instructs.add(Instruction(record[0], record[1].toInt(),
                    record[2].toInt(), record[3].toInt(), record[4].toInt()))
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

/*
  *  add 1 to cycles if page boundery is crossed
  ** add 1 to cycles if branch occurs on same page
     add 2 to cycles if branch occurs to different page
*/

}

