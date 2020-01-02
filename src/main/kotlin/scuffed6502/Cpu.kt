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
        private const val RESET_V   = 0xFFFC                    // Reset vector
        private const val IRQ_V     = 0xFFFE                    // IRQ vector
        private const val NMI_V     = 0xFFFA                    // NMI vector
        private const val SP_RESET  = 0xFD                      //
    }

    private val instructions: List<Instruction> = initInstructionSet(INSTRUCTION_FILE)
    private val memory: IntArray = IntArray(MEMSIZE)

    // Registers
    private var regPC = 0        // program counter
    private var regSP = SP_LOC   // stack pointer
    private var regA  = 0        // accumulator
    private var regX  = 0        // register x
    private var regY  = 0        // register y

    // Flags (Status Register)   NVUB DIZC
    private var flagC = 0;  private val maskC = (1 shl 0) // carry
    private var flagZ = 0;  private val maskZ = (1 shl 1) // zero
    private var flagI = 0;  private val maskI = (1 shl 2) // disable interrupt
    private var flagD = 0;  private val maskD = (1 shl 3) // decimal (BCD) mode
    private var flagB = 0;  private val maskB = (1 shl 4) // break command
    private var flagU = 1;  private val maskU = (1 shl 5) // unused
    private var flagV = 0;  private val maskV = (1 shl 6) // overflow
    private var flagN = 0;  private val maskN = (1 shl 7) // negative

    // Helpers
    private var addrAbs = 0x0000
    private var addrRel = 0x0000
    private var temp = 0x0000
    private var fetched = 0x00
    private var cycles = 0
    private var opcode = 0


    // Reset to known state
    fun reset(){
        addrAbs = RESET_V
        regPC = readWord(RESET_V)
        regA = 0; regX = 0; regY = 0
        regSP = SP_RESET
        setStatus(0x00 or maskU)

        addrRel = 0x0000
        addrAbs = 0x0000
        fetched = 0x00
        cycles = 8
    }

    // Interrupt request
    fun irq(){ // TODO DRY
        if(flagI == 0){
            // Push program counter and status flags to stack
            pushStack((regPC ushr 8) and 0x00FF)
            pushStack(regSP and 0x00FF)
            flagB = 0; flagU = 1; flagI = 1
            pushStack(getStatus())

            // Read new pc location
            addrAbs = IRQ_V
            regPC = readWord(addrAbs)
            cycles = 7
        }
    }

    // Non-maskable interrupt
    fun nmi(){ // TODO DRY
        pushStack((regPC ushr 8) and 0x00FF)
        pushStack(regSP and 0x00FF)
        flagB = 0; flagU = 1; flagI = 1
        pushStack(getStatus())

        addrAbs = NMI_V
        regPC = readWord(addrAbs)
        cycles = 8
    }

    fun clock(){
        if(cycles == 0){
            opcode = readByte(regPC)
            val instruction = getInstructionByOpcode(opcode)
            flagU = 1
            regPC++

            cycles = instruction.cycles
            val addrCycles = addressingModeHandler(instruction.mode)
            val opCycles = opcodeHandler(instruction.opcode)
            cycles += (addrCycles and opCycles)
            flagU = 1
        }
        cycles--
    }

    private fun fetch(): Int{
        if(getInstructionByOpcode(opcode).mode != AddrMode.IMP){
            fetched = readByte(addrAbs)
        }
        return fetched
    }

    private fun readByte(addr: Int): Int{  // TODO read from bus
        if((addr >= 0x0000) and (addr <= 0xFFFF)) {
            return memory[addr and 0xFFFF]
        } else {
            throw cpuError("Out of bounds, cannot read address '$addr'")
        }
    }
    private fun readWord(addr: Int):Int = readByte(addr) or (readByte(addr + 1) shl 8)

    private fun writeByte(addr: Int, data: Int){  // TODO write to bus
        if((addr >= 0x0000) and (addr <= 0xFFFF)) {
            memory[addr and 0xFFFF] = data
        } else {
            throw cpuError("Out of bounds, cannot write data '$data' to address '$addr'")
        }
    }

    fun loadProgram(rom: List<Int>, pgmEntry: Int, resetHi: Int = 0x00, resetLo: Int = 0x80): Map<Int,Disassembly>{
        if(pgmEntry < 0){
            throw Exception("Program entry point '$pgmEntry' out of bounds. [0-$MEMSIZE]")
        } else if((pgmEntry + rom.size) > MEMSIZE){
            throw Exception("ROM size too large (${rom.size}) for entry point $pgmEntry")
        }
        rom.forEachIndexed{i,_ -> memory[i + pgmEntry] = rom[i] }
        writeByte(RESET_V, resetHi)
        writeByte(RESET_V+1, resetLo)
        // TODO IRQ,NMI ?
        val disassembly = disassemble(0x0000, 0xFFFF)
        reset()
        return disassembly
    }

    // Disassemble memory within a range
    fun disassemble(start: Int, stop: Int): Map<Int,Disassembly>{
        val disassembled: MutableMap<Int,Disassembly> = mutableMapOf()
        var addr = start; var lineAddr: Int

        if(start > stop){
            throw Exception("Cannot start at position greater than stopping position")
        } else if(start < 0 || start > MEMSIZE){
            throw Exception("Start position '$start' out of bounds")
        } else if(stop < 0 || stop > MEMSIZE){
            throw Exception("Stop position '$stop' out of bounds")
        }
        while(addr <= stop){
            lineAddr = addr
            val instruction = getInstructionByOpcode(readByte(addr))
            val hex: MutableList<Int> = mutableListOf(instruction.opcode)
            var asm = "$%04X".format(addr++) + ":    ${instruction.mnemonic} "

            asm += when (instruction.mode) {
                AddrMode.IMP -> ""
                AddrMode.ACC -> ""
                AddrMode.IMM -> {
                    hex.add(readByte(addr++))
                    "#$%02X".format(hex[1])
                }
                AddrMode.ZP0 -> {
                    hex.add(readByte(addr++))
                    "$%02X".format(hex[1])
                }
                AddrMode.ZPX -> {
                    hex.add(readByte(addr++))
                    "$%02X".format(hex[1]) + ", X"
                }
                AddrMode.ZPY -> {
                    hex.add(readByte(addr++))
                    "$%02X".format(hex[1]) + ", Y"
                }
                AddrMode.IZX -> {
                    hex.add(readByte(addr++))
                    "($%02X".format(hex[1]) + ", X)"
                }
                AddrMode.IZY -> {
                    hex.add(readByte(addr++))
                    "($%02X".format(hex[1]) + ", Y)"
                }
                AddrMode.ABS -> {
                    hex.addAll(listOf(readByte(addr++), readByte(addr++)))
                    "$%04X".format((hex[2] shl 8) or hex[1])
                }
                AddrMode.ABX -> {
                    hex.addAll(listOf(readByte(addr++), readByte(addr++)))
                    "$%04X".format((hex[2] shl 8) or hex[1]) + ", X"
                }
                AddrMode.ABY -> {
                    hex.addAll(listOf(readByte(addr++), readByte(addr++)))
                    "$%04X".format((hex[2] shl 8) or hex[1]) + ", Y"
                }
                AddrMode.IND -> {
                    hex.addAll(listOf(readByte(addr++), readByte(addr++)))
                    "($%04X".format((hex[2] shl 8) or hex[1]) + ")"
                }
                AddrMode.REL -> {
                    hex.add(readByte(addr++))
                    "$%02X".format(hex[1]) + " [$%04X".format(addr + hex[1]) + "]"
                }
            }
            disassembled[lineAddr] = Disassembly(lineAddr, asm.padEnd(25, ' '),
                instruction, hex.joinToString(" "){"%02X".format(it)})
        }
        return disassembled
    }


    /******************************************** Instructions ***********************************************/
    private fun opcodeHandler(opcode: Int): Int{
        return when(getInstructionByOpcode(opcode).mnemonic){
            "ADC" -> opADC();    "AND" -> opAND();    "ASL" -> opASL();    "BCC" -> opBCC()
            "BCS" -> opBCS();    "BEQ" -> opBEQ();    "BIT" -> opBIT();    "BMI" -> opBMI()
            "BNE" -> opBNE();    "BPL" -> opBPL();    "BRK" -> opBRK();    "BVC" -> opBVC()
            "BVS" -> opBVS();    "CLC" -> opCLC();    "CLD" -> opCLD();    "CLI" -> opCLI()
            "CLV" -> opCLV();    "CMP" -> opCMP();    "CPX" -> opCPX();    "CPY" -> opCPY()
            "DEC" -> opDEC();    "DEX" -> opDEX();    "DEY" -> opDEY();    "EOR" -> opEOR()
            "INC" -> opINC();    "INX" -> opINX();    "INY" -> opINY();    "JMP" -> opJMP()
            "JSR" -> opJSR();    "LDA" -> opLDA();    "LDX" -> opLDX();    "LDY" -> opLDY()
            "LSR" -> opLSR();    "NOP" -> opNOP();    "ORA" -> opORA();    "PHA" -> opPHA()
            "PHP" -> opPHP();    "PLA" -> opPLA();    "PLP" -> opPLP();    "ROL" -> opROL()
            "ROR" -> opROR();    "RTI" -> opRTI();    "RTS" -> opRTS();    "SBC" -> opSBC()
            "SEC" -> opSEC();    "SED" -> opSED();    "SEI" -> opSEI();    "STA" -> opSTA()
            "STX" -> opSTX();    "STY" -> opSTY();    "TAX" -> opTAX();    "TAY" -> opTAY()
            "TSX" -> opTSX();    "TXA" -> opTXA();    "TXS" -> opTXS();    "TYA" -> opTYA()
            else  -> opXXX()
        }
    }

    // Add with carry
    private fun opADC():Int{
        fetch()
        temp = regA + fetched + flagC
        flagC = if(temp > 255) 1 else 0
        flagZ = if((temp and 0x00FF) == 0) 1 else 0
        flagV = if((((regA xor fetched).inv()) and (regA xor temp) and 0x0080) == 1) 1 else 0
        flagN = if((temp and 0x80) == 1) 1 else 0
        regA = temp and 0x00FF
        return 1
    }

    // Bitwise AND (with accumulator)
    private fun opAND():Int{
        fetch()
        regA = regA and fetched
        flagZ = if(regA == 0x00) 1 else 0
        flagN = if((regA and 0x80) == 1) 1 else 0
        return 1
    }

    // Arithmetic Shift Left
    private fun opASL():Int{
        fetch()
        temp = fetched shl 1
        flagC = if((temp and 0xFF00) > 0) 1 else 0
        flagZ = if((temp and 0x00FF) == 0x00) 1 else 0
        flagN = if((temp and 0x80) == 1) 1 else 0
        if(getAddrModeByOpcode(opcode) == AddrMode.IMP){
            regA = temp and 0x00FF
        } else {
            writeByte(addrAbs, temp and 0x00FF)
        }
        return 0
    }

    // Branch if carry clear
    private fun opBCC():Int{
        if(flagC == 0){
            cycles++
            addrAbs = regPC + addrRel
            if((addrAbs and 0xFF00) != (regPC and 0xFF00)){
                cycles++
            }
            regPC = addrAbs
        }
        return 0
    }

    // Branch if carry set
    private fun opBCS():Int{
        if(flagC == 1){
            cycles++
            addrAbs = regPC + addrRel
            if((addrAbs and 0xFF00) != (regPC and 0xFF00)){
                cycles++
            }
            regPC = addrAbs
        }
        return 0
    }

    // Branch if equal (zero set)
    private fun opBEQ():Int{
        if(flagZ == 1){
            cycles++
            addrAbs = regPC + addrRel
            if((addrAbs and 0xFF00) != (regPC and 0xFF00)){
                cycles++
            }
            regPC = addrAbs
        }
        return 0
    }

    // Bit test
    private fun opBIT():Int{
        fetch()
        temp = regA and fetched
        flagZ = if((temp and 0x00FF) == 0x00) 1 else 0
        flagN = if((fetched and (1 shl 7)) == 1) 1 else 0
        flagV = if((fetched and (1 shl 6)) == 1) 1 else 0
        return 0
    }

    // Branch on minus (negative set)
    private fun opBMI():Int{
        if(flagN == 1){
            cycles++
            addrAbs = regPC + addrRel
            if((addrAbs and 0xFF00) != (regPC and 0xFF00)){
                cycles++
            }
            regPC = addrAbs
        }
        return 0
    }

    // Branch if not equal (zero clear)
    private fun opBNE():Int{
        if(flagZ == 0){
            cycles++
            addrAbs = regPC + addrRel
            if((addrAbs and 0xFF00) != (regPC and 0xFF00)){
                cycles++
            }
            regPC = addrAbs
        }
        return 0
    }

    // Branch if positive (negative clear)
    private fun opBPL():Int{
        if(flagN == 0){
            cycles++
            addrAbs = regPC + addrRel
            if((addrAbs and 0xFF00) != (regPC and 0xFF00)){
                cycles++
            }
            regPC = addrAbs
        }
        return 0
    }

    // Break
    private fun opBRK():Int{
        regPC++
        flagI = 1
        pushStack((regPC ushr 8) and 0x00FF)
        pushStack(regPC and 0x00FF)
        flagB = 1
        pushStack(getStatus())
        flagB = 0
        regPC = readWord(IRQ_V)
        return 0
    }

    // Branch if overflow clear
    private fun opBVC():Int{
        if(flagV == 0){
            cycles++
            addrAbs = regPC + addrRel
            if((addrAbs and 0xFF00) != (regPC and 0xFF00)){
                cycles++
            }
            regPC = addrAbs
        }
        return 0
    }

    // Branch if overflow set
    private fun opBVS():Int{
        if(flagV == 1){
            cycles++
            addrAbs = regPC + addrRel
            if((addrAbs and 0xFF00) != (regPC and 0xFF00)){
                cycles++
            }
            regPC = addrAbs
        }
        return 0
    }

    // Clear carry
    private fun opCLC():Int{
        flagC = 0
        return 0
    }

    // Clear decimal
    private fun opCLD():Int{
        flagD = 0
        return 0
    }

    // Clear interrupt
    private fun opCLI():Int{
        flagI = 0
        return 0
    }

    // Clear overflow
    private fun opCLV():Int{
        flagV = 0
        return 0
    }

    // Compare (with accumulator)
    private fun opCMP():Int{
        fetch()
        temp = regA - fetched
        flagC = if(regA >= fetched) 1 else 0
        flagZ = if((temp and 0x00FF) == 0x0000) 1 else 0
        flagN = if((temp and 0x0080) == 1) 1 else 0
        return 1
    }

    // Compare X register
    private fun opCPX():Int{
        fetch()
        temp = regX - fetched
        flagC = if(regX >= fetched) 1 else 0
        flagZ = if((temp and 0x00FF) == 0x0000) 1 else 0
        flagN = if((temp and 0x0080) == 1) 1 else 0
        return 0
    }

    // Compare Y register
    private fun opCPY():Int{
        fetch()
        temp = regY - fetched
        flagC = if(regY >= fetched) 1 else 0
        flagZ = if((temp and 0x00FF) == 0x0000) 1 else 0
        flagN = if((temp and 0x0080) == 1) 1 else 0
        return 0
    }

    // Decrement (at memory location)
    private fun opDEC():Int{
        fetch()
        temp = fetched - 1
        writeByte(addrAbs, temp and 0x00FF)
        flagZ = if((temp and 0x00FF) == 0x0000) 1 else 0
        flagN = if((temp and 0x0080) == 1) 1 else 0
        return 0
    }

    // Decrement X register
    private fun opDEX():Int{
        regX--
        flagZ = if(regX == 0x00) 1 else 0
        flagN = if((regX and 0x80) == 1) 1 else 0
        return 0
    }

    // Decrement Y register
    private fun opDEY():Int{
        regY--
        flagZ = if(regY == 0x00) 1 else 0
        flagN = if((regY and 0x80) == 1) 1 else 0
        return 0
    }

    // Bitwise XOR (with accumulator)
    private fun opEOR():Int{
        fetch()
        regA = regA xor fetched
        flagZ = if(regA == 0x00) 1 else 0
        flagN = if((regA and 0x80) == 1) 1 else 0
        return 1
    }

    // Increment (at memory location)
    private fun opINC():Int{
        fetch()
        temp = fetched + 1
        writeByte(addrAbs, temp and 0x00FF)
        flagZ = if((temp and 0x00FF) == 0x0000) 1 else 0
        flagN = if((temp and 0x0080) == 1) 1 else 0
        return 0
    }

    // Increment X register
    private fun opINX():Int{
        regX++
        flagZ = if(regX == 0x00) 1 else 0
        flagN = if((regX and 0x80) == 1) 1 else 0
        return 0
    }

    // Increment Y register
    private fun opINY():Int{
        regY++
        flagZ = if(regY == 0x00) 1 else 0
        flagN = if((regY and 0x80) == 1) 1 else 0
        return 0
    }

    // Jump
    private fun opJMP():Int{
        regPC = addrAbs
        return 0
    }

    // Jump to subroutine
    private fun opJSR():Int{
        regPC--
        pushStack((regPC ushr 8) and 0x00FF)
        pushStack(regPC and 0x00FF)
        regPC = addrAbs
        return 0
    }

    // Load accumulator
    private fun opLDA():Int{
        fetch()
        regA = fetched
        flagZ = if(regA == 0x00) 1 else 0
        flagN = if((regA and 0x80) == 1) 1 else 0
        return 1
    }

    // Load X register
    private fun opLDX():Int{
        fetch()
        regX = fetched
        flagZ = if(regX == 0x00) 1 else 0
        flagN = if((regX and 0x80) == 1) 1 else 0
        return 1
    }

    // Load Y register
    private fun opLDY():Int{
        fetch()
        regY = fetched
        flagZ = if(regY == 0x00) 1 else 0
        flagN = if((regY and 0x80) == 1) 1 else 0
        return 1
    }

    // Logical Shift Right
    private fun opLSR():Int{
        fetch()
        flagC = if((fetched and 0x0001) == 1) 1 else 0
        temp = fetched ushr 1
        flagZ = if((temp and 0x00FF) == 0x0000) 1 else 0
        flagN = if((temp and 0x0080) == 1) 1 else 0
        if(getAddrModeByOpcode(opcode) == AddrMode.IMP){
            regA = temp and 0x00FF
        } else {
            writeByte(addrAbs, temp and 0x00FF)
        }
        return 0
    }

    // No operation  (TODO add in illegal opcodes)
    private fun opNOP():Int{
        return when(opcode){
            0x1C-> 1;   0x3C-> 1;   0x5C-> 1;   0x7C-> 1;
            0xDC-> 1;   0xFC-> 1;   else-> 0
        }
    }

    // Biwise OR (with accumulator)
    private fun opORA():Int{
        fetch()
        regA = regA or fetched
        flagZ = if(regA == 0x00) 1 else 0
        flagN = if((regA and 0x80) == 1) 1 else 0
        return 1
    }

    // Push accumulator (to stack)
    private fun opPHA():Int{
        pushStack(regA)
        return 0
    }

    // Push status register (to stack)
    private fun opPHP():Int{
        pushStack(getStatus() or maskB or maskU)
        flagB = 0
        flagU = 0
        return 0
    }

    // Pop accumulator (from stack)
    private fun opPLA():Int{
        regA = popStack()
        flagZ = if(regA == 0x00) 1 else 0
        flagN = if((regA and 0x80) == 1) 1 else 0
        return 0
    }

    // Pop status register (from stack)
    private fun opPLP():Int{
        setStatus(popStack())
        flagU = 1
        return 0
    }

    // Rotate Left
    private fun opROL():Int{
        fetch()
        temp = (fetched shl 1) or flagC
        flagC = if((temp and 0xFF00) == 1) 1 else 0
        flagZ = if((temp and 0x00FF) == 0x0000) 1 else 0
        flagN = if((temp and 0x0080) == 1) 1 else 0
        if(getAddrModeByOpcode(opcode) == AddrMode.IMP){
            regA = temp and 0x00FF
        } else{
            writeByte(addrAbs, temp and 0x00FF)
        }
        return 0
    }

    // Rotate Right
    private fun opROR():Int{
        fetch()
        temp = (flagC shl 7) or (fetched ushr 1)
        flagC = if((fetched and 0x01) == 1) 1 else 0
        flagZ = if((temp and 0x00FF) == 0x00) 1 else 0
        flagN = if((temp and 0x0080) == 1) 1 else 0
        if(getAddrModeByOpcode(opcode) == AddrMode.IMP){
            regA = temp and 0x00FF
        } else{
            writeByte(addrAbs, temp and 0x00FF)
        }
        return 0
    }

    // Return from interrupt
    private fun opRTI():Int{
        setStatus(popStack())
        setStatus((getStatus() and maskB.inv()))
        setStatus((getStatus() and maskU.inv()))
        regPC = popStack() or (popStack() shl 8)
        return 0
    }

    // Return from subroutine
    private fun opRTS():Int{
        regPC = popStack() or (popStack() shl 8)
        regPC++
        return 0
    }

    // Subtract with carry
    private fun opSBC():Int{
        fetch()
        val value = fetched xor 0x00FF
        temp = regA + value + flagC
        flagC = if((temp and 0xFF00) == 1) 1 else 0
        flagZ = if((temp and 0x00FF) == 0) 1 else 0
        flagV = if(((temp xor regA) and (temp xor value) and 0x0080) == 1) 1 else 0
        flagN = if((temp and 0x0080) == 1) 1 else 0
        regA = temp and 0x00FF
        return 1
    }

    // Set carry
    private fun opSEC():Int{
        flagC = 1
        return 0
    }

    // Set decimal
    private fun opSED():Int{
        flagD = 1
        return 0
    }

    // Set interrupt
    private fun opSEI():Int{
        flagI = 1
        return 0
    }

    // Store accumulator (at address)
    private fun opSTA():Int{
        writeByte(addrAbs, regA)
        return 0
    }

    // Store X register (at address)
    private fun opSTX():Int{
        writeByte(addrAbs, regX)
        return 0
    }

    // Store Y register (at address)
    private fun opSTY():Int{
        writeByte(addrAbs, regY)
        return 0
    }

    // Transfer accumulator to X register
    private fun opTAX():Int{
        regX = regA
        flagZ = if(regX == 0x00) 1 else 0
        flagN = if((regX and 0x80) == 1) 1 else 0
        return 0
    }

    // Transfer accumulator to Y register
    private fun opTAY():Int{
        regY = regA
        flagZ = if(regY == 0x00) 1 else 0
        flagN = if((regY and 0x80) == 1) 1 else 0
        return 0
    }

    // Transfer stack pointer to X register
    private fun opTSX():Int{
        regX = regSP
        flagZ = if(regX == 0x00) 1 else 0
        flagN = if((regX and 0x80) == 1) 1 else 0
        return 0
    }

    // Transfer X register to Accumulator
    private fun opTXA():Int{
        regA = regX
        flagZ = if(regA == 0x00) 1 else 0
        flagN = if((regA and 0x80) == 1) 1 else 0
        return 0
    }

    // Transfer X register to stack pointer
    private fun opTXS():Int{
        regSP = regX
        return 0
    }

    // Transfer Y register to Accumulator
    private fun opTYA():Int{
        regA = regY
        flagZ = if(regA == 0x00) 1 else 0
        flagN = if((regA and 0x80) == 1) 1 else 0
        return 0
    }

    // Illegal opcodes
    private fun opXXX():Int{
        throw cpuError("Illegal opcode encountered")
    }



    /****************************************** Addressing Modes *********************************************/
    private fun addressingModeHandler(mode: AddrMode): Int{
        return when(mode){
            AddrMode.IMM -> addrIMM();    AddrMode.ABS -> addrABS();    AddrMode.ZP0 -> addrZP0()
            AddrMode.ABX -> addrABX();    AddrMode.ABY -> addrABY();    AddrMode.ZPX -> addrZPX()
            AddrMode.ZPY -> addrZPY();    AddrMode.IZX -> addrIZX();    AddrMode.IZY -> addrIZY()
            AddrMode.REL -> addrREL();    AddrMode.IMP -> addrIMP();    AddrMode.ACC -> addrACC()
            AddrMode.IND -> addrIND()
        }
    }

    private fun addrIMM(): Int{
        addrAbs = regPC++
        return 0
    }

    private fun addrABS(): Int{
        val lo = readByte(regPC)
        regPC++
        val hi = readByte(regPC)
        regPC++
        addrAbs = (hi shl 8) or lo
        return 0
    }

    private fun addrZP0(): Int{
        addrAbs = readByte(regPC)
        regPC++
        addrAbs = addrAbs and 0x00FF
        return 0
    }

    private fun addrABX(): Int{
        val lo = readByte(regPC)
        regPC++
        val hi = readByte(regPC)
        regPC++

        addrAbs = (hi shl 8) or lo
        addrAbs += regX
        return if((addrAbs and 0xFF00) != (hi shl 8)) 1 else 0 // page boundary
    }

    private fun addrABY(): Int{
        val lo = readByte(regPC)
        regPC++
        val hi = readByte(regPC)
        regPC++

        addrAbs = (hi shl 8) or lo
        addrAbs += regY
        return if((addrAbs and 0xFF00) != (hi shl 8)) 1 else 0 // page boundary
    }

    private fun addrZPX(): Int{
        addrAbs = readByte(regPC) + regX
        regPC++
        addrAbs = addrAbs and 0x00FF
        return 0
    }

    private fun addrZPY(): Int{
        addrAbs = readByte(regPC) + regY
        regPC++
        addrAbs = addrAbs and 0x00FF
        return 0
    }

    private fun addrIZX(): Int{
        val t = readByte(regPC)
        regPC++
        val lo = readByte((t + regX) and 0x00FF)
        val hi = readByte((t + regX + 1) and 0x00FF)
        addrAbs = (hi shl 8) or lo
        return 0
    }

    private fun addrIZY(): Int{
        val t = readByte(regPC)
        regPC++
        val lo = readByte(t and 0x00FF)
        val hi = readByte((t + 1) and 0x00FF)
        addrAbs = (hi shl 8) or lo
        addrAbs += regY
        return if((addrAbs and 0xFF00) != (hi shl 8)) 1 else 0
    }

    // address must be within -128 to +127 of branch instruction
    private fun addrREL(): Int{
        addrRel = readByte(regPC)
        regPC++
        if((addrRel and 0x80) == 1){
            addrRel = addrRel or 0xFF00
        }
        return 0
    }

    private fun addrIMP(): Int{
        fetched = regA
        return 0
    }

    private fun addrACC(): Int{
        fetched = regA
        return 0
    }

    private fun addrIND(): Int{
        val lo = readByte(regPC)
        regPC++
        val hi = readByte(regPC)
        regPC++
        val addr = (hi shl 8) or lo

        addrAbs = if(lo == 0x00FF){  // Simulate page boundary bug
            (readByte(addr and 0xFF00) shl 8) or readByte(addr)
        } else {
            (readByte(addr + 1) shl 8) or readByte(addr)
        }
        return 0
    }


    /****************************************** Stack and Status *********************************************/
    private fun peekStack(): Int{
        return memory[regSP + SP_LOC]
    }

    private fun pokeStack(data: Int){
        memory[regSP + SP_LOC] = data
    }

    private fun pushStack(data: Int){
        writeByte(regSP + SP_LOC, data)
        regSP--
    }

    private fun popStack(): Int{
        regSP++
        return readByte(regSP + SP_LOC)
    }

    // Flags to status register conversions
    fun getStatus() = (flagC shl 0) or (flagZ shl 1) or (flagI shl 2) or (flagD shl 3) or
            (flagB shl 4) or (flagU shl 5) or (flagV shl 6) or (flagN shl 7) or 0

    fun setStatus(status: Int){
        flagC = 1 and (status ushr 0); flagZ = 1 and (status ushr 1)
        flagI = 1 and (status ushr 2); flagD = 1 and (status ushr 3)
        flagB = 1 and (status ushr 4); flagU = 1 and (status ushr 5)
        flagV = 1 and (status ushr 6); flagN = 1 and (status ushr 7)
    }


    /****************************************** Utils and Accessors ******************************************/
    fun getInstructionSet() = instructions
    fun getMemory() = memory
    fun getCycles() = cycles

    fun getFlag(f: String): Int{
        return when(f.toUpperCase()){
            "N"-> flagN;   "V"-> flagV;   "U"-> flagU;   "B"-> flagB
            "D"-> flagD;   "I"-> flagI;   "Z"-> flagZ;   "C"-> flagC
            else -> throw cpuError("'$f' is not a valid flag [N,V,U,B,D,I,Z,C]")
        }
    }

    fun peek(addr: Int) = memory[addr] // Read memory with no cycle
    fun wipeMemory() = memory.forEachIndexed{i,_ -> memory[i] = 0x00}

    private fun getInstructionByOpcode(opcode: Int): Instruction = instructions.find {it.opcode == opcode} ?: throw cpuError("Invalid opcode '$opcode'")
    private fun getAddrModeByOpcode(opcode: Int) = getInstructionByOpcode(opcode).mode
    private fun cpuError(msg: String): CpuException = CpuException(msg, opcode, getStatus())

    private fun initInstructionSet(fileName: String): List<Instruction>{
        val instructions : MutableList<Instruction> = mutableListOf()
        val fileReader = BufferedReader(InputStreamReader(this.javaClass.getResourceAsStream("/$fileName")))
        val csvReader = CSVReaderBuilder(fileReader).withSkipLines(1).build()
        try{
            csvReader.readAll().forEach{record ->
                instructions.add(Instruction(record[0].toUpperCase(), record[1].toInt(16),
                    AddrMode.valueOf(record[2].toUpperCase()), record[3].toInt()))
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
