package scuffed6502

import com.fasterxml.jackson.module.kotlin.*

@ExperimentalUnsignedTypes
@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")
class Cpu(val bus: Bus): IDevice {

    var regAcc: UByte = 0U
    var regX: UByte = 0U
    var regY: UByte = 0U
    var stkPtr: UByte = 0U
    var pc: UShort = 0U
    var regStatus: UByte = 0U
    private var fetched: UByte = 0U
    private var addrAbs: UShort = 0U
    private var addrRel: UShort = 0U // Branch within a certain distance of originating call
    private var opcode: UByte = 0U
    private var cycles: UByte = 0U

    var logData: MutableList<Map<String,String>> = mutableListOf()

    val instructions: List<Instruction> = jacksonObjectMapper()
            .readValue(javaClass.classLoader.getResource("Cpu.json").readText())

    val flags = mutableMapOf(
            "C" to false, "Z" to false, "I" to false, "D" to false,
            "B" to false, "U" to false, "V" to false, "N" to false
    )

    fun getFlag(f: String): Boolean{
        return flags[f.toUpperCase()] ?: false
    }

    fun setFlag(f: String, v: Boolean){
        flags[f.toUpperCase()] = v
        var bin = ""
        flags.forEach{ (k, v) ->
            bin += (if(v) "1" else "0")
        }
        regStatus = Integer.parseInt(bin,2).toUByte()
    }

    override fun read(addr: UShort): UByte = this.bus.readFromRAM(addr)

    override fun write(addr: UShort, data: UByte): Boolean = this.bus.writeToRAM(addr, data)

    override fun connectBus(bus: Bus){
        bus.connectDevice(this)
    }

    override fun disconnectBus() {
        bus.disconnectDevice(this)
    }

    fun complete() = cycles == 0U.toUByte()

    fun tickClock(){
        if(cycles == 0U.toUByte()){
            opcode = read(pc)
            setFlag("U", true)
            pc++
            cycles = instructions[opcode.toInt()].cycles.toUByte()
            val moreClock1 = addressModeHandler(instructions[opcode.toInt()].addrMode)
            val moreClock2 = instructionHandler(instructions[opcode.toInt()].impl)
            cycles = (cycles + (moreClock1 and moreClock2).toUByte()).toUByte()
            setFlag("U", true)
        }
        cycles--
    }

    private fun log(){
        logData.add(mapOf(
                "PC" to "%04x".format(if(logData.size > 0) pc.toInt() else 0x8000U.toInt()).toUpperCase(),
                "OPCODE" to instructions[opcode.toInt()].mnemonic,
                "ACC" to "%02x".format(regAcc.toInt()).toUpperCase(),
                "X" to "%02x".format(regX.toInt()).toUpperCase(),
                "Y" to "%02x".format(regY.toInt()).toUpperCase(),
                "StkPtr" to "%02x".format(stkPtr.toInt()).toUpperCase(),
                "C" to getFlag("C").toInt().toString(), "Z" to getFlag("Z").toInt().toString(),
                "I" to getFlag("I").toInt().toString(), "D" to getFlag("D").toInt().toString(),
                "B" to getFlag("B").toInt().toString(), "U" to getFlag("U").toInt().toString(),
                "V" to getFlag("V").toInt().toString(), "N" to getFlag("N").toInt().toString()
        ))
    }

    fun disassemble(start: UShort, end: UShort): MutableMap<UShort, String> {
        var addr = start
        var value: UByte
        var lo: UByte
        var hi: UByte
        var disassembled: MutableMap<UShort, String> = mutableMapOf()
        var lineAddr: UShort
        var i = 0 //start

        while(i.toUInt() <= end.toUInt()){//addr <= end){
            lineAddr = addr
            var instruction = "$%02x".format(addr.toInt()) + ": "
            val opcode = read(addr.toUShort())
            addr++
            i++

            val addrMode = instructions[opcode.toInt()].addrMode
            instruction += instructions[opcode.toInt()].mnemonic + " "
            when (addrMode) {
                "IMP" -> instruction += " {IMP}"
                "IMM" -> {
                    value = read(addr.toUShort())
                    addr++
                    instruction += "#$%02x".format(value.toInt()) + " {IMM}"
                }
                "ZP0" -> {
                    lo = read(addr.toUShort())
                    addr++
                    instruction += "$%02x".format(lo.toInt()) + " {ZP0}"
                }
                "ZPX" -> {
                    lo = read(addr.toUShort())
                    addr++
                    instruction += "$%02x".format(lo.toInt()) + ", X {ZPX}"
                }
                "ZPY" -> {
                    lo = read(addr.toUShort())
                    addr++
                    instruction += "$%02x".format(lo.toInt()) + " {ZPY}"
                }
                "IZX" -> {
                    lo = read(addr.toUShort())
                    addr++
                    instruction += "$%02x".format(lo.toInt()) + " {IZX}"
                }
                "IZY" -> {
                    lo = read(addr.toUShort())
                    addr++
                    instruction += "$%02x".format(lo.toInt()) + " {IZY}"
                }
                "ABS" -> {
                    lo = read(addr.toUShort());
                    addr++
                    hi = read(addr.toUShort());
                    addr++
                    instruction += "$%04x".format(((hi shl 8).toUShort() or lo.toUShort()).toInt()) + " {ABS}"
                }
                "ABX" -> {
                    lo = read(addr.toUShort());
                    addr++
                    hi = read(addr.toUShort());
                    addr++
                    instruction += "$%04x".format(((hi shl 8).toUShort() or lo.toUShort()).toInt()) + " {ABX}"
                }
                "ABY" -> {
                    lo = read(addr.toUShort());
                    addr++
                    hi = read(addr.toUShort());
                    addr++
                    instruction += "$%04x".format(((hi shl 8).toUShort() or lo.toUShort()).toInt()) + " {ABY}"
                }
                "IND" -> {
                    lo = read(addr.toUShort());
                    addr++
                    hi = read(addr.toUShort());
                    addr++
                    instruction += "$%04x".format(((hi shl 8).toUShort() or lo.toUShort()).toInt()) + " {IND}"
                }
                "REL" -> {
                    value = read(addr.toUShort());
                    addr++
                    instruction += "$%02x".format(value.toInt()) + " [$%04x".format((addr + value).toInt()) + "] {REL}"
                }
            }
            disassembled[lineAddr] = instruction
        }
        return disassembled
    }

    private fun fetch(): UByte {
        if(instructions[opcode.toInt()].addrMode != "IMP"){
            fetched = read(addrAbs)
        }
        return fetched
    }

    private fun addressModeHandler(addrMode: String): UByte{
        return when(addrMode) {
            "IMP" -> addrIMP();     "IMM" -> addrIMM();    "ZP0" -> addrZP0();    "ZPX" -> addrZPX()
            "ZPY" -> addrZPY();     "REL" -> addrREL();    "ABS" -> addrABS();    "ABX" -> addrABX()
            "ABY" -> addrABY();     "IND" -> addrIND();    "IZX" -> addrIZX();    "IZY" -> addrIZY()
            else  -> 0U
        }
    }

    private fun instructionHandler(impl: String): UByte{
        val result = when(impl) {
            "ADC" -> insADC();      "AND" -> insAND();      "ASL" -> insASL();      "BCC" -> insBCC()
            "BCS" -> insBCS();      "BEQ" -> insBEQ();      "BIT" -> insBIT();      "BMI" -> insBMI()
            "BNE" -> insBNE();      "BPL" -> insBPL();      "BRK" -> insBRK();      "BVC" -> insBVC()
            "BVS" -> insBVS();      "CLC" -> insCLC();      "CLD" -> insCLD();      "CLI" -> insCLI()
            "CLV" -> insCLV();      "CMP" -> insCMP();      "CPX" -> insCPX();      "CPY" -> insCPY()
            "DEC" -> insDEC();      "DEX" -> insDEX();      "DEY" -> insDEY();      "EOR" -> insEOR()
            "INC" -> insINC();      "INX" -> insINX();      "INY" -> insINY();      "JMP" -> insJMP()
            "JSR" -> insJSR();      "LDA" -> insLDA();      "LDX" -> insLDX();      "LDY" -> insLDY()
            "LSR" -> insLSR();      "NOP" -> insNOP();      "ORA" -> insORA();      "PHA" -> insPHA()
            "PHP" -> insPHP();      "PLA" -> insPLA();      "PLP" -> insPLP();      "ROL" -> insROL()
            "ROR" -> insROR();      "RTI" -> insRTI();      "RTS" -> insRTS();      "SBC" -> insSBC()
            "SEC" -> insSEC();      "SED" -> insSED();      "SEI" -> insSEI();      "STA" -> insSTA()
            "STX" -> insSTX();      "STY" -> insSTY();      "TAX" -> insTAX();      "TAY" -> insTAY()
            "TSX" -> insTSX();      "TXA" -> insTXA();      "TXS" -> insTXS();      "TYA" -> insTYA()
            else  -> insXXX()
        }
        val m = instructions[opcode.toInt()].mnemonic
        if(pc.toInt() > 10) {
            //println("executing instruction $m")
        }
        log()
        return result
    }

    private fun storeRegister(value: UByte): UByte{
        write(addrAbs, value)
        return 0U
    }

    // Reset CPU state. Jumps to value at location 0xFFFC at compile
    fun reset(){
        addrAbs = 0xFFFCU
        val lo = read((addrAbs + 0U).toUShort()).toUShort()
        val hi = read((addrAbs + 1U).toUShort()).toUShort()
        pc = (hi shl 8) or lo
        regAcc = 0U
        regX = 0U
        regY = 0U
        stkPtr = 0xFDU
        regStatus = 0x00U.toUByte() or getFlag("U").toUByte()
        addrRel = 0x0000U
        addrAbs = 0x0000U
        fetched = 0x00U

        cycles = 8U
    }

    // Execute instruction at location 0xFFFE
    fun interruptReq(){
        if(!getFlag("I")){
            write((0x0100U.toUByte() + stkPtr).toUShort(), ((pc ushr 8) and 0x00FFU.toUShort()).toUByte())
            stkPtr--
            write((0x0100U.toUByte() + stkPtr).toUShort(), (pc and 0x00FFU.toUShort()).toUByte())
            stkPtr--

            setFlag("B", false)
            setFlag("U", true)
            setFlag("I", true)
            write((0x0100U.toUShort() + stkPtr).toUShort(), regStatus)
            stkPtr--

            addrAbs = 0xFFFEU
            val lo = read(addrAbs.plus(0U).toUShort()).toUShort()
            val hi = read(addrAbs.plus(1U).toUShort()).toUShort()
            pc = (hi shl 8) or lo

            cycles = 7U
        }
    }

    // Execute instruction at location 0xFFFA, cannot be disabled
    fun nmInterruptReq(){
        write((0x0100U.toUByte() + stkPtr).toUShort(), ((pc ushr 8) and 0x00FF.toUShort()).toUByte())
        stkPtr--
        write((0x0100U.toUByte() + stkPtr).toUShort(), (pc and 0x00FF.toUShort()).toUByte())
        stkPtr--

        setFlag("B", false)
        setFlag("U", true)
        setFlag("I", true)
        write((0x0100U.toUByte() + stkPtr).toUShort(), regStatus)
        stkPtr--

        addrAbs = 0xFFFAU
        val lo = read(addrAbs.plus(0U).toUShort()).toUShort()
        val hi = read(addrAbs.plus(1U).toUShort()).toUShort()
        pc = (hi shl 8) or lo
        cycles = 8U
    }



    // Addressing Modes - http://www.obelisk.me.uk/6502/addressing.html

    // Implied -> Target accumulator
    private fun addrIMP(): UByte {
        fetched = regAcc
        return 0U
    }

    // Immediate -> Next byte as value, point address to next byte in instruction
    private fun addrIMM(): UByte {
        addrAbs = pc++
        return 0U
    }

    // Zero Page -> Address location absolutely using only first 256 bytes of memory (1 byte address)
    private fun addrZP0(): UByte {
        addrAbs = read(pc++).toUShort().and(0x00FFU.toUShort())
        return 0U
    }

    // Zero Page X -> Zero page addressing plus X register value as offset
    private fun addrZPX(): UByte {
        addrAbs = read(pc++).plus(regX).toUShort().and(0x00FFU.toUShort())
        return 0U
    }

    // Zero Page Y -> Zero page addressing plus Y register value as offset
    private fun addrZPY(): UByte {
        addrAbs = read(pc++).plus(regY).toUShort().and(0x00FFU.toUShort())
        return 0U
    }

    // Relative -> Address to branch from must be within 1byte relative offset (branch instructions)
    private fun addrREL(): UByte {
        addrRel = read(pc++).toUShort()
        if(addrRel.and(0x80U.toUShort()) == 1U.toUShort()){
            addrRel = addrRel.or(0xFF00U.toUShort())
        }
        return 0U
    }

    // Absolute -> Address using full 2 byte address
    private fun addrABS(): UByte {
        val lo = read(pc++).toUShort()
        val hi = read(pc++).toUShort()
        addrAbs = (hi shl 8) or lo
        return 0U
    }

    // Absolute X -> absolute addressing with x register value as offset
    private fun addrABX(): UByte {
        val lo = read(pc++).toUShort()
        val hi = read(pc++).toUShort()
        addrAbs = (hi shl 8).or(lo).plus(regX).toUShort()
        return if (addrAbs.and(0xFF00U.toUShort()) != (hi shl 8)) 1U else 0U
    }

    // Absolute Y -> absolute addressing with y register value as offset
    private fun addrABY(): UByte {
        val lo = read(pc++).toUShort()
        val hi = read(pc++).toUShort()
        addrAbs = (hi shl 8).or(lo).plus(regY).toUShort()
        return if (addrAbs.and(0xFF00U.toUShort()) != (hi shl 8)) 1U else 0U
    }

    // Indirect -> use 2 byte address to get actual 2 byte address
    private fun addrIND(): UByte {
        val lo = read(pc++).toUShort()
        val hi = read(pc++).toUShort()
        val ptr = (hi shl 8).or(lo)
        addrAbs = if(lo == 0x00FF.toUShort()){ // simulate page boundary hardware bug
            (read((ptr.and(0xFF00U.toUShort())) shl 8)).or(read(ptr)).toUShort()
        } else {
            (read(((ptr.plus(1U)) shl 8).toUShort())).or(read(ptr)).toUShort()
        }
        return 0U
    }

    // Indirect X -> use 1 byte address plus x register value as offset for address on zero page
    private fun addrIZX(): UByte {
        val t = read(pc++).toUShort()
        val lo = read((t + regX).toUShort() and(0x00FFU.toUShort()).toUShort()).toUShort()
        val hi = read((t + regX + 1U).toUShort() and(0x00FFU.toUShort()).toUShort()).toUShort()
        addrAbs = (hi shl 8).or(lo)
        return 0U
    }

    // Indirect Y -> use 1 byte address plus y register value as offset for address on zero page
    private fun addrIZY(): UByte {
        val t = read(pc++).toUShort()
        val lo = read(t.and(0x00FFU.toUShort())).toUShort()
        val hi = read((t + 1U).toUShort() and(0x00FFU.toUShort()).toUShort()).toUShort()
        addrAbs = (hi shl 8).or(lo).plus(regY).toUShort()
        return if((addrAbs and(0xFF00U.toUShort())) == (hi shl 8)) 1U else 0U
    }



    // Instruction Helpers

    private fun branchInstruction(condition: Boolean): UByte {
        if(condition) {
            cycles++
            addrAbs = (pc + addrRel).toUShort()
            if ((addrAbs and 0xFF00U.toUShort()).toUShort() != (pc and 0xFF00U.toUShort()).toUShort()) {
                cycles++
            }
            println("!!!!!!!!! $addrAbs, $pc")
            pc = addrAbs
        }
        return 0U
    }

    private fun updateFlag(f: String, v: Boolean): UByte {
        setFlag(f, v)
        return 0U
    }

    private fun compareRegister(reg: UByte): UByte{
        fetch()
        val regUShort = reg.toUShort()
        val tmp = (regUShort - fetched.toUShort()).toUShort()
        setFlag("C", regUShort >= fetched)
        setFlag("Z", (tmp and 0x00FFU.toUShort()) == 0x0000.toUShort())
        setFlag("N", (tmp and 0x0080U.toUShort()).equalsInt(1))
        return 0U
    }

    private fun incrementRegister(reg: UByte, value: Int): UByte{
        val regNew = (if(value > 0) (reg + value.toUByte()) else (reg - value.toUByte())).toUByte()
        setFlag("Z", regNew == 0x00.toUByte())
        setFlag("N", (regNew and 0x80U.toUByte()).equalsInt(1))
        return regNew
    }

    private fun loadRegister(): UByte{
        fetch()
        setFlag("Z", fetched == 0x00.toUByte())
        setFlag("N", (fetched and 0x80U.toUByte()).equalsInt(1))
        return fetched
    }



    // Instructions

    // Add with carry in
    private fun insADC(): UByte {
        fetch()
        val tmp = (regAcc.toUShort() + fetched.toUShort() + getFlag("C").toUShort()).toUShort()
        setFlag("C", tmp > 255U)
        setFlag("Z", (tmp and 0x00FFU.toUShort()).equalsInt(0))
        setFlag("N", (tmp and 0x80U.toUShort()).equalsInt(1))
        setFlag("V", (((regAcc.toUShort() xor fetched.toUShort()).inv()) and (regAcc.toUShort() xor tmp) and 0x0080U.toUShort()).equalsInt(1))
        regAcc = (tmp and 0x00FFU.toUShort()).toUByte()
        return 1U
    }

    // Logical AND
    private fun insAND(): UByte {
        fetch()
        regAcc = regAcc and fetched
        setFlag("Z", regAcc == 0x00.toUByte())
        setFlag("N", (regAcc and 0x80U.toUByte()).equalsInt(1))
        return 1U
    }

    // Arithmetic shift left
    private fun insASL(): UByte {
        fetch()
        val tmp = (fetched.toUShort() shl 1)
        setFlag("C", (tmp and 0xFF00U.toUShort()) > 0U)
        setFlag("Z", (tmp and 0x00FFU.toUShort()) == 0x00.toUShort())
        setFlag("N", (tmp and 0x80U.toUShort()).equalsInt(1))
        if(instructions[opcode.toInt()].addrMode == "IMP"){
            regAcc = (tmp and 0x00FFU.toUShort()).toUByte()
        } else {
            write(addrAbs, (tmp and 0x00FFU.toUShort()).toUByte())
        }
        return 0U
    }

    // Branch if carry clear
    private fun insBCC() = branchInstruction(!getFlag("C"))

    // Branch if carry set
    private fun insBCS() = branchInstruction(getFlag("C"))

    // Branch if equal
    private fun insBEQ() = branchInstruction(getFlag("Z"))

    // Bit test
    private fun insBIT(): UByte {
        fetch()
        val tmp = (regAcc and fetched).toUShort()
        setFlag("Z", (tmp and 0x00FFU.toUShort()) == 0x00.toUShort())
        setFlag("N", (fetched and ((1U shl 7).toUByte())).equalsInt(1))
        setFlag("V", (fetched and ((1U shl 6).toUByte())).equalsInt(1))
        return 0U
    }

    // Branch if negative
    private fun insBMI() = branchInstruction(getFlag("N"))

    // Branch if not equal
    private fun insBNE() = branchInstruction(!getFlag("Z"))

    // Branch if positive
    private fun insBPL() = branchInstruction(!getFlag("N"))

    // Program sourced interrupt
    private fun insBRK(): UByte {
        pc++
        setFlag("I", true)
        write((0x0100U.toUByte() + stkPtr).toUShort(), ((pc ushr 8) and 0x00FFU.toUShort()).toUByte())
        stkPtr--
        write((0x0100U.toUByte() + stkPtr).toUShort(), (pc and 0x00FFU.toUShort()).toUByte())
        stkPtr--

        setFlag("B", true)
        write((0x0100U.toUByte() + stkPtr).toUShort(), regStatus)
        stkPtr--
        setFlag("B", false)
        pc = (read(0xFFFEU.toUShort()).toUShort() or ((read(0xFFFFU.toUShort()).toUShort() shl 8).toUByte()).toUShort())

        return 0U
    }

    // Branch if overflow clear
    private fun insBVC() = branchInstruction(!getFlag("V"))

    // Branch if overflow set
    private fun insBVS() = branchInstruction(getFlag("V"))

    // Clear carry flag
    private fun insCLC() = updateFlag("C", false)

    // Clear decimal flag
    private fun insCLD() = updateFlag("D", false)

    // Disable interrupts, clear interrupt flag
    private fun insCLI() = updateFlag("I", false)

    // Clear overflow flag
    private fun insCLV() = updateFlag("V", false)

    // Compare accumulator
    private fun insCMP() = compareRegister(regAcc)

    // Compare X register
    private fun insCPX() = compareRegister(regX)

    // Compare Y register
    private fun insCPY() = compareRegister(regY)

    // Decrement value at memory location
    private fun insDEC(): UByte {
        fetch()
        val tmp = (fetched - 1U).toUShort()
        write(addrAbs, (tmp and 0x00FFU.toUShort()).toUByte())
        setFlag("Z", (tmp and 0x00FFU.toUShort()) == 0x0000.toUShort())
        return 0U
    }

    // Decrement X register
    private fun insDEX(): UByte {
        regX = incrementRegister(regX, -1)
        return 0U
    }

    // Decrement Y register
    private fun insDEY(): UByte {
        regY = incrementRegister(regY, -1)
        return 0U
    }

    // Logical Exclusive OR
    private fun insEOR(): UByte {
        fetch()
        regAcc = regAcc xor fetched
        setFlag("Z", regAcc == 0x00.toUByte())
        setFlag("N", (regAcc and 0x80U.toUByte()).equalsInt(1))
        return 1U
    }

    // Increment value at memory location
    private fun insINC(): UByte {
        fetch()
        val tmp = (fetched + 1U).toUShort()
        write(addrAbs, (tmp and 0x00FFU.toUShort()).toUByte())
        setFlag("Z", (tmp and 0x00FFU.toUShort()) == 0x0000.toUShort())
        setFlag("N", (tmp and 0x0080U.toUShort()).equalsInt(1))
        return 0U
    }

    // Increment X register
    private fun insINX(): UByte {
        regX = incrementRegister(regX, 1)
        return 0U
    }

    // Increment Y register
    private fun insINY(): UByte {
        regY = incrementRegister(regY, 1)
        return 0U
    }

    // Jump to location
    private fun insJMP(): UByte {
        pc = addrAbs
        return 0U
    }

    // Jump to subroutine
    private fun insJSR(): UByte {
        pc--
        write((0x0100U.toUShort() + stkPtr).toUShort(), ((pc ushr 8) and 0x00FFU.toUShort()).toUByte())
        stkPtr--
        write((0x0100U.toUShort() + stkPtr).toUShort(), (pc and 0x00FFU.toUShort()).toUByte())
        stkPtr--
        pc = addrAbs
        return 0U
    }

    // Load accumulator
    private fun insLDA(): UByte {
        regAcc = loadRegister()
        return 1U
    }

    // Load X register
    private fun insLDX(): UByte {
        regX = loadRegister()
        return 1U
    }

    // Load Y register
    private fun insLDY(): UByte {
        regY = loadRegister()
        return 1U
    }

    // Logical Shift Right
    private fun insLSR(): UByte {
        fetch()
        setFlag("C", (fetched and 0x0001U.toUByte()).equalsInt(1))
        val tmp = (fetched ushr 1).toUShort()
        setFlag("Z", (tmp and 0x00FFU.toUShort()).toUShort() == 0x0000.toUShort())
        if(instructions[opcode.toInt()].addrMode == "IMP"){
            regAcc = (tmp and 0x00FFU.toUShort()).toUByte()
        } else {
            write(addrAbs, (tmp and 0x00FFU.toUShort()).toUByte())
        }
        return 0U
    }

    // No operation. Not all NOPs are equal -> https://wiki.nesdev.com/w/index.php/CPU_unofficialopcodes
    private fun insNOP(): UByte {
        return when(opcode){
            0x1C.toUByte() -> 1U
            0x3C.toUByte() -> 1U
            0x5C.toUByte() -> 1U
            0x7C.toUByte() -> 1U
            0xDC.toUByte() -> 1U
            0xFC.toUByte() -> 1U
            else -> 0U
        }
    }

    // Bitwise OR
    private fun insORA(): UByte {
        fetch()
        regAcc = regAcc or fetched
        setFlag("Z", regAcc == 0x00.toUByte())
        setFlag("N", (regAcc and 0x80U.toUByte()).equalsInt(1))
        return 1U
    }

    // Push accumulator to stack
    private fun insPHA(): UByte {
        write((0x0100U.toUShort() + stkPtr).toUShort(), regAcc)
        stkPtr--
        return 0U
    }

    // Push status register to stack
    private fun insPHP(): UByte {
        stkPtr++
        regStatus = read((0x0100U.toUByte() + stkPtr).toUShort())
        setFlag("U", true)
        return 0U
    }

    // Pop accumulator off stack
    private fun insPLA(): UByte {
        stkPtr++
        regAcc = read((0x0100U.toUByte() + stkPtr).toUShort())
        setFlag("Z", regAcc == 0x00.toUByte())
        setFlag("N", (regAcc and 0x80U.toUByte()).equalsInt(1))
        return 0U
    }

    // Pop status register off stack
    private fun insPLP(): UByte {
        stkPtr++
        regStatus = read((0x0100U.toUByte() + stkPtr).toUShort())
        setFlag("U", true)
        return 0U
    }

    // Rotate left
    private fun insROL(): UByte {
        fetch()
        val tmp = (fetched shl 1).toUShort() or getFlag("C").toUShort()
        setFlag("C", (tmp and 0xFF00U.toUShort()).equalsInt(1))
        setFlag("Z", (tmp and 0x00FFU.toUShort()) == 0x0000.toUShort())
        setFlag("N", (tmp and 0x0080U.toUShort()).equalsInt(1))
        if(instructions[opcode.toInt()].addrMode == "IMP"){
            regAcc = (tmp and 0x00FFU.toUShort()).toUByte()
        } else {
            write(addrAbs, (tmp and 0x00FFU.toUShort()).toUByte())
        }
        return 0U
    }

    // Rotate right
    private fun insROR(): UByte {
        fetch()
        val tmp = (getFlag("C").toUByte() shl 7).toUShort() or ((fetched ushr 1).toUShort())
        setFlag("C", (fetched and 0x01U.toUByte()).equalsInt(1))
        setFlag("Z", (tmp and 0x00FFU.toUShort()) == 0x00.toUShort())
        setFlag("N", (tmp and 0x0080U.toUShort()).equalsInt(1))
        if(instructions[opcode.toInt()].addrMode == "IMP"){
            regAcc = (tmp and 0x00FFU.toUShort()).toUByte()
        } else {
            write(addrAbs, (tmp and 0x00FFU.toUShort()).toUByte())
        }
        return 0U
    }

    // Return from interrupt
    private fun insRTI(): UByte {
        stkPtr++
        regStatus = read((0x0100U.toUByte() + stkPtr).toUShort())
        regStatus = regStatus and getFlag("B").toUByte().inv()
        regStatus = regStatus and getFlag("U").toUByte().inv()

        stkPtr++
        pc = read((0x0100U.toUByte() + stkPtr).toUShort()).toUShort()
        stkPtr++
        pc = pc or (read((0x0100U.toUByte() + stkPtr).toUShort()).toUShort() shl 8)
        return 0U
    }

    // Return from subroutine
    private fun insRTS(): UByte {
        stkPtr++
        pc = read((0x0100U.toUByte() + stkPtr).toUShort()).toUShort()
        stkPtr++
        pc = pc or (read((0x0100U.toUByte() + stkPtr).toUShort()).toUShort() shl 8)
        pc++
        return 0U
    }

    // Subtract with borrow in
    private fun insSBC(): UByte {
        fetch()
        val value = fetched.toUShort() xor 0x00FFU.toUShort()
        val tmp = (regAcc.toUShort() + value + getFlag("C").toUShort()).toUShort()
        setFlag("C", tmp > 255U)
        setFlag("Z", (tmp and 0x00FFU.toUShort()).equalsInt(0))
        setFlag("N", (tmp and 0x80U.toUShort()).equalsInt(1))
        setFlag("V", (((regAcc.toUShort() xor fetched.toUShort()).inv()) and (regAcc.toUShort() xor tmp) and 0x0080U.toUShort()).equalsInt(1))
        regAcc = (tmp and 0x00FFU.toUShort()).toUByte()
        return 1U
    }

    // Set carry flag
    private fun insSEC() = updateFlag("C", true)

    // Set decimal flag
    private fun insSED() = updateFlag("D", true)

    // Set interrupt flag / enable interrupts
    private fun insSEI() = updateFlag("I", true)

    // Store accumulator at address
    private fun insSTA() = storeRegister(regAcc)

    // Store register x at address
    private fun insSTX() = storeRegister(regX)

    // Store register y at address
    private fun insSTY() = storeRegister(regY)

    // Transfer accumulator to X register
    private fun insTAX(): UByte {
        regX = regAcc
        setFlag("Z", regX == 0x00.toUByte())
        setFlag("N", (regX and 0x80U.toUByte()).equalsInt(1))
        return 0U
    }

    // Transfer accumulator to Y register
    private fun insTAY(): UByte {
        regY = regAcc
        setFlag("Z", regY == 0x00.toUByte())
        setFlag("N", (regY and 0x80U.toUByte()).equalsInt(1))
        return 0U
    }

    // Transfer stack point to X register
    private fun insTSX(): UByte {
        regX = stkPtr
        setFlag("Z", regX == 0x00.toUByte())
        setFlag("N", (regY and 0x80U.toUByte()).equalsInt(1))
        return 0U
    }

    // Transfer X register to accumulator
    private fun insTXA(): UByte {
        regAcc = regX
        setFlag("Z", regAcc == 0x00.toUByte())
        setFlag("N", (regAcc and 0x80U.toUByte()).equalsInt(1))
        return 0U
    }

    // Transfer X register to stack pointer
    private fun insTXS(): UByte {
        stkPtr = regX
        return 0U
    }

    // Transfer Y register to accumulator
    private fun insTYA(): UByte {
        regAcc = regY
        setFlag("Z", regAcc == 0x00.toUByte())
        setFlag("N", (regAcc and 0x80U.toUByte()).equalsInt(1))
        return 0U
    }

    // illegal opcodes caught here
    private fun insXXX() = 0U.toUByte()


}



// Extension Methods

private fun Boolean.toInt() = if(this) 1 else 0

@ExperimentalUnsignedTypes
private infix fun UShort.shl(i: Int) = ((this.toInt()) shl i).toUShort()

@ExperimentalUnsignedTypes
private infix fun UShort.ushr(i: Int) = ((this.toInt()) ushr i).toUShort()

@ExperimentalUnsignedTypes
private infix fun UByte.shl(i: Int) = ((this.toInt()) shl i).toUByte()

@ExperimentalUnsignedTypes
private infix fun UByte.ushr(i: Int) = ((this.toInt()) ushr i).toUByte()

@ExperimentalUnsignedTypes
private fun UByte.equalsInt(i: Int) = this.toInt() == i

@ExperimentalUnsignedTypes
private fun UShort.equalsInt(i: Int) = this.toInt() == i

@ExperimentalUnsignedTypes
private fun Boolean.toUShort() = if (this) 1U.toUShort() else 0U.toUShort()

@ExperimentalUnsignedTypes
private fun Boolean.toUByte() = if (this) 1U.toUByte() else 0U.toUByte()