package scuffed6502

class Cpu {

    companion object {
        private const val MEMSIZE = 0x10000 // 65536; ram, audio, graphics, IO sections
    }

    // enum for addressing mode
    // enum for interrupt types

    private var memory: IntArray = IntArray(MEMSIZE)

    // Registers
    private var regPC: Int = 0      // program counter
    private var regSP: Int = 0xFF   // stack pointer
    private var regA:  Int = 0      // accumulator
    private var regX:  Int = 0      // register x
    private var regY:  Int = 0      // register y

    // Flags
    private var flagC = 0           // carry flag
    private var flagZ = 0           // zero flag
    private var flagI = 0           // interrupt disable flag
    private var flagD = 0           // decimal mode flag
    private var flagB = 0           // break command flag
    private var flagU = 0           // unused flag
    private var flagV = 0           // overflow flag
    private var flagN = 0           // negative flag



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

    // TODO: load in instruction set from a CSV
    private fun loadInstructionSet(path: String){

    }

}