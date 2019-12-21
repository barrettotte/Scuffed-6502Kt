package scuffed6502

import com.opencsv.CSVReaderBuilder
import java.io.BufferedReader
import java.io.InputStreamReader

@Suppress("SameParameterValue")
class Cpu {

    companion object {
        private const val INSTRUCTION_FILE = "instructions.csv" // /resources/instructions.csv
        private const val MEMSIZE   = 0x10000                   // 65536; ram, audio, graphics, IO sections
        private const val SP_LOC    = 0x100                     // location of stack in memory
        private const val RESET_VL  = 0xFFFC                    // Reset vector low
        private const val SP_RESET  = 0x1FD                     //
    }

    private val instructions: List<Instruction> = initInstructionSet(INSTRUCTION_FILE)
    private val memory: IntArray = IntArray(MEMSIZE)

    // Registers
    private var regSP = SP_LOC // stack pointer TODO: might need to be bounded to 0x00-0xFF ?
    private var regPC = 0      // program counter
    private var regA  = 0      // accumulator
    private var regX  = 0      // register x
    private var regY  = 0      // register y

    // Flags
    private var flagC = 0      // carry flag
    private var flagZ = 0      // zero flag
    private var flagI = 0      // disable interrupt flag
    private var flagD = 0      // decimal (BCD) mode flag (1 -> byte represents number from 0-99)
    private var flagB = 0      // break command flag
    private var flagU = 0      // unused flag
    private var flagV = 0      // overflow flag
    private var flagN = 0      // negative flag

    // Interrupts
    private var intNMI = false // non-maskable interrupt signal
    private var intIRQ = false // interrupt request signal

    // Helpers
    private var maskInterrupt = false
    private var interrupt = false
    private var currentOpcode = 0
    private var cycles = 0

    fun reset(){
        cycles = 0
        regSP = SP_RESET                                     // Reset program counter to
        regSP = (memory[regSP] or (memory[regSP + 1] shl 8)) //  address in reset vector
        currentOpcode = memory[regSP]
        maskInterrupt = true
        intIRQ = false; intNMI = false
    }

    // Return flags as a byte
    fun getStatus() = (flagC shl 0) or (flagZ shl 1) or (flagI shl 2) or (flagD shl 3) or
                      (flagB shl 4) or (flagU shl 5) or (flagV shl 6) or (flagN shl 7) or 0

    fun setStatus(status: Int){
        flagC = 1 and (status shr 0); flagZ = 1 and (status shr 1)
        flagI = 1 and (status shr 2); flagD = 1 and (status shr 3)
        flagB = 1 and (status shr 4); flagU = 1 and (status shr 5)
        flagV = 1 and (status shr 6); flagN = 1 and (status shr 7)
    }

    fun read(addr: Int): Int{
        val data = memory[addr]
        cycle()
        return data
    }

    fun write(addr: Int, data: Int){
        cycle()
        memory[addr] = data
    }

    private fun cycle(){
        cycles++
        interrupt = (intNMI || (intIRQ && !maskInterrupt))
    }

    fun step(){
        currentOpcode = read(regPC++)
        opcodeHandler(currentOpcode)
        if(interrupt){
            if(intNMI){
                // TODO NMI
            } else if(intIRQ){
                // TODO IRQ
            }
        }
    }


    // Addressing
    private fun getAddress(mode: AddrMode): Int{
        return when(mode){
            AddrMode.IMM -> addrIMM();    AddrMode.ABS -> addrABS();    AddrMode.ZP0 -> addrZP0()
            AddrMode.ABX -> addrABX();    AddrMode.ABY -> addrABY();    AddrMode.ZPX -> addrZPX()
            AddrMode.ZPY -> addrZPY();    AddrMode.IZX -> addrIZX();    AddrMode.IZY -> addrIZY();
            AddrMode.REL -> addrREL();    else -> 0 // Addressing modes that don't need an address [IMP,ACC,IND]
        }
    }

    private fun addrIMM() = regPC++
    private fun addrABS() = read(regPC++) or (read(regPC++) shl 8)
    private fun addrZP0() = read(regPC++)
    private fun addrREL() = regPC

    private fun addrABX(): Int{
        val lo = read(regPC++)
        val hi = read(regPC++)
        if(lo + regX > 0xFF){         // ASL, DEC, INC, LSR, ROL, ROR, STA
            if(currentOpcode in arrayOf(0x1E,0xDE,0xFE,0x5E,0x3E,0x7E,0x9D)){
                return ((hi shl 8 or lo) + regX) and 0xFFFF
            } else{
                read((((hi shl 8 or lo) + regX) - 0xFF) and 0xFFFF)
            }
        }
        return ((hi shl 8 or lo) + regX) and 0xFFFF
    }

    private fun addrABY(): Int{
        val lo = read(regPC++)
        val hi = read(regPC++)                // STA
        if(lo + regY > 0xFF && currentOpcode != 0x99){
            read((((hi shl 8 or lo) + regY) - 0xFF) and 0xFFFF)
        }
        return ((hi shl 8 or lo) + regY) and 0xFFFF
    }

    private fun addrZPX(): Int{
        var addr = read(regPC++)
        read(addr)
        addr = (addr + regX) and 0xFF
        return if(addr > 0xFF) addr - 0x100 else addr
    }

    private fun addrZPY(): Int{
        val addr = read(regPC++)
        read(addr)
        return (addr + regY) and 0xFF
    }

    private fun addrIZX(): Int{
        var addr = read(regPC++)
        read(addr)
        addr += regX
        return read((addr and 0xFF)) or (read((addr + 1) and 0xFF) shl 8)
    }

    private fun addrIZY(): Int{
        val tmp = read(regPC++)
        val addr = read(tmp) + (read((tmp + 1) and 0xFF) shl 8)
        if((addr and 0xFF) + regY > 0xFF && currentOpcode != 0x91){
            read((addr + regY - 0xFF) and 0xFFFF)     // STA
        }
        return (addr + regY) and 0xFFFF
    }


    // Instructions
    private fun opcodeHandler(opcode: Int){
        val ins = getInstructionByOpcode(opcode)
        when(val mnemonic = ins.mnemonic){
            "ADC" -> opADC(ins);    "AND" -> opAND(ins);    "ASL" -> opASL(ins);    "BCC" -> opBCC(ins)
            "BCS" -> opBCS(ins);    "BEQ" -> opBEQ(ins);    "BIT" -> opBIT(ins);    "BMI" -> opBMI(ins)
            "BNE" -> opBNE(ins);    "BPL" -> opBPL(ins);    "BRK" -> opBRK(ins);    "BVC" -> opBVC(ins)
            "BVS" -> opBVS(ins);    "CLC" -> opCLC(ins);    "CLD" -> opCLD(ins);    "CLI" -> opCLI(ins)
            "CLV" -> opCLV(ins);    "CMP" -> opCMP(ins);    "CPX" -> opCPX(ins);    "CPY" -> opCPY(ins)
            "DEC" -> opDEC(ins);    "DEX" -> opDEX(ins);    "DEY" -> opDEY(ins);    "EOR" -> opEOR(ins)
            "INC" -> opINC(ins);    "INX" -> opINX(ins);    "INY" -> opINY(ins);    "JMP" -> opJMP(ins)
            "JSR" -> opJSR(ins);    "LDA" -> opLDA(ins);    "LDX" -> opLDX(ins);    "LDY" -> opLDY(ins)
            "LSR" -> opLSR(ins);    "NOP" -> opNOP(ins);    "ORA" -> opORA(ins);    "PHA" -> opPHA(ins)
            "PHP" -> opPHP(ins);    "PLA" -> opPLA(ins);    "PLP" -> opPLP(ins);    "ROL" -> opROL(ins)
            "ROR" -> opROR(ins);    "RTI" -> opRTI(ins);    "RTS" -> opRTS(ins);    "SBC" -> opSBC(ins)
            "SEC" -> opSEC(ins);    "SED" -> opSED(ins);    "SEI" -> opSEI(ins);    "STA" -> opSTA(ins)
            "STX" -> opSTX(ins);    "STY" -> opSTY(ins);    "TAX" -> opTAX(ins);    "TAY" -> opTAY(ins)
            "TSX" -> opTSX(ins);    "TXA" -> opTXA(ins);    "TXS" -> opTXS(ins);    "TYA" -> opTYA(ins)
            "???" -> throw Exception("ERROR: Unknown instruction '$mnemonic'.")
        }
    }

    private fun opADC(ins: Instruction) = ""
    private fun opAND(ins: Instruction) = ""
    private fun opASL(ins: Instruction) = ""
    private fun opBCC(ins: Instruction) = ""
    private fun opBCS(ins: Instruction) = ""
    private fun opBEQ(ins: Instruction) = ""
    private fun opBIT(ins: Instruction) = ""
    private fun opBMI(ins: Instruction) = ""
    private fun opBNE(ins: Instruction) = ""
    private fun opBPL(ins: Instruction) = ""
    private fun opBRK(ins: Instruction) = ""
    private fun opBVC(ins: Instruction) = ""
    private fun opBVS(ins: Instruction) = ""
    private fun opCLC(ins: Instruction) = ""
    private fun opCLD(ins: Instruction) = ""
    private fun opCLI(ins: Instruction) = ""
    private fun opCLV(ins: Instruction) = ""
    private fun opCMP(ins: Instruction) = ""
    private fun opCPX(ins: Instruction) = ""
    private fun opCPY(ins: Instruction) = ""
    private fun opDEC(ins: Instruction) = ""
    private fun opDEX(ins: Instruction) = ""
    private fun opDEY(ins: Instruction) = ""
    private fun opEOR(ins: Instruction) = ""
    private fun opINC(ins: Instruction) = ""
    private fun opINX(ins: Instruction) = ""
    private fun opINY(ins: Instruction) = ""
    private fun opJMP(ins: Instruction) = ""
    private fun opJSR(ins: Instruction) = ""
    private fun opLDA(ins: Instruction) = ""
    private fun opLDX(ins: Instruction) = ""
    private fun opLDY(ins: Instruction) = ""
    private fun opLSR(ins: Instruction) = ""
    private fun opNOP(ins: Instruction) = ""
    private fun opORA(ins: Instruction) = ""
    private fun opPHA(ins: Instruction) = ""
    private fun opPHP(ins: Instruction) = ""
    private fun opPLA(ins: Instruction) = ""
    private fun opPLP(ins: Instruction) = ""
    private fun opROL(ins: Instruction) = ""
    private fun opROR(ins: Instruction) = ""
    private fun opRTI(ins: Instruction) = ""
    private fun opRTS(ins: Instruction) = ""
    private fun opSBC(ins: Instruction) = ""
    private fun opSEC(ins: Instruction) = ""
    private fun opSED(ins: Instruction) = ""
    private fun opSEI(ins: Instruction) = ""
    private fun opSTA(ins: Instruction) = ""
    private fun opSTX(ins: Instruction) = ""
    private fun opSTY(ins: Instruction) = ""
    private fun opTAX(ins: Instruction) = ""
    private fun opTAY(ins: Instruction) = ""
    private fun opTSX(ins: Instruction) = ""
    private fun opTXA(ins: Instruction) = ""
    private fun opTXS(ins: Instruction) = ""
    private fun opTYA(ins: Instruction) = ""


    // Stack
    fun peekStack(): Int{
        return memory[regSP + SP_LOC]
    }
    fun pokeStack(data: Int){
        memory[regSP + SP_LOC] = data
    }
    fun pushStack(data: Int){
        // TODO
    }
    fun popStack(): Int{
        return 0
    }

    // Accessors
    fun getInstructionSet() = instructions
    fun getMemory() = memory
    fun getCycles() = cycles

    // Utils
    fun peek(addr: Int) = memory[addr]
    fun wipeMemory() = memory.forEachIndexed{_,i -> memory[i] = 0x00}
    private fun getInstructionByOpcode(opcode: Int) = instructions.find{it.opcode == opcode} ?: Instruction()

    private fun initInstructionSet(fileName: String): List<Instruction>{
        val instructions : MutableList<Instruction> = mutableListOf()
        val fileReader = BufferedReader(InputStreamReader(this.javaClass.getResourceAsStream("/$fileName")))
        val csvReader = CSVReaderBuilder(fileReader).withSkipLines(1).build()
        try{
            csvReader.readAll().forEach{record ->
                instructions.add(Instruction(record[0].toUpperCase(), record[1].toInt(16),
                    record[2].toInt(), record[3].toInt(), AddrMode.valueOf(record[4].toUpperCase())))
            }
        } catch(e: Exception){
            println("Error loading instructions.")
            e.printStackTrace()
        } finally{
            fileReader.close()
            csvReader!!.close()
        }
        return instructions
    }

}

