package scuffed6502

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.*
import java.io.File
import kotlin.experimental.or

@ExperimentalUnsignedTypes
@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")
class Cpu: IDevice{

    enum class FLAGS(val bit: Int){
        C(0), // Carry Bit
        Z(1), // Zero
        I(2), // Disable interrupts
        D(3), // Decimal mode [unused]
        B(4), // Break
        U(5), // Unused
        V(6), // Overflow
        N(7); // Negative
        companion object{ fun getMask(obj: FLAGS): UByte = (1U shl(obj.bit)).toUByte() }
    } // is this necessary?


    private var bus: Bus? = null
    private var regAcc: UByte = 0u
    private var regX: UByte = 0u
    private var regY: UByte = 0u
    private var stkPtr: UByte = 0u
    private var pc: UShort = 0u
    private var regStatus: UByte = 0u

    private var fetched: UByte = 0u

    private var addrAbs: UShort = 0u

    // In the 6502, branching can only occur within a certain distance of originating call
    private var addrRel: UShort = 0u

    private var opcode: UByte = 0u
    private var cycles: UByte = 0u


    // list of 256 instructions
    var instructions: List<Instruction>



    override fun connectBus(bus: Bus){
        bus.connectDevice(this)
        this.bus = bus
    }

    override fun disconnectBus() {
        bus?.disconnectDevice(this)
        this.bus = null
    }

    override fun read(addr: UShort): UByte = this.bus?.readFromRAM(addr) ?: 0U

    override fun write(addr: UShort, data: UByte): Boolean = this.bus?.writeToRAM(addr, data) ?: false


    // Access status register
    fun getFlag(f: FLAGS) = 1
    fun setFlag(f: FLAGS, v: Boolean) = 1

    fun reset() = 1

    // Execute instruction at specific location
    fun interruptReq() = 1

    // Execute instruction at specific location, cannot be disabled
    fun nmInterruptReq() = 1

    fun fetch() = 1U


    // Opcodes
    fun ADC() = 1U
    fun AND() = 1U
    fun ASL() = 1U
    fun BCC() = 1U
    fun BCS() = 1U
    fun BEQ() = 1U
    fun BIT() = 1U
    fun BMI() = 1U
    fun BNE() = 1U
    fun BPL() = 1U
    fun BRK() = 1U
    fun BVC() = 1U
    fun BVS() = 1U
    fun CLC() = 1U
    fun CLD() = 1U
    fun CLI() = 1U
    fun CLV() = 1U
    fun CMP() = 1U
    fun CPX() = 1U
    fun CPY() = 1U
    fun DEC() = 1U
    fun DEX() = 1U
    fun DEY() = 1U
    fun EOR() = 1U
    fun INC() = 1U
    fun INX() = 1U
    fun INY() = 1U
    fun JMP() = 1U
    fun JSR() = 1U
    fun LDA() = 1U
    fun LDX() = 1U
    fun LDY() = 1U
    fun LSR() = 1U
    fun NOP() = 1U
    fun ORA() = 1U
    fun PHA() = 1U
    fun PHP() = 1U
    fun PLA() = 1U
    fun PLP() = 1U
    fun ROL() = 1U
    fun ROR() = 1U
    fun RTI() = 1U
    fun RTS() = 1U
    fun SBC() = 1U
    fun SEC() = 1U
    fun SED() = 1U
    fun SEI() = 1U
    fun STA() = 1U
    fun STX() = 1U
    fun STY() = 1U
    fun TAX() = 1U
    fun TAY() = 1U
    fun TSX() = 1U
    fun TXA() = 1U
    fun TXS() = 1U
    fun TYA() = 1U

    // illegal opcode
    fun XXX() = 1


    init {
        instructions = jacksonObjectMapper().readValue(javaClass.classLoader.getResource("Cpu.json").readText())
    }

    fun tickClock(){
        if(cycles.toUInt() == 0U){
            val lookup = opcode.toInt()
            opcode = bus?.readFromRAM(pc++) ?: 0U
            cycles = instructions[lookup].cycles.toUByte()
            val moreClock1 = addressModeHandler(instructions[lookup].addrMode)
            val moreClock2 = instructionHandler(instructions[lookup].impl)
            cycles = cycles.plus(moreClock1.and(moreClock2)).toUByte()
        }
        cycles--
    }

    private fun addressModeHandler(addrMode: String): UByte{
        return 0U
    }
    private fun instructionHandler(impl: String): UByte{
        return 0U
    }


    // Addressing Modes - http://www.obelisk.me.uk/6502/addressing.html

    // Implied -> Target accumulator
    private fun IMP(): UByte {
        fetched = regAcc
        return 0U
    }

    // Immediate -> Next byte as value, point address to next byte in instruction
    private fun IMM(): UByte {
        addrAbs = pc++
        return 0U
    }

    // Zero Page -> Address location absolutely using only first 256 bytes of memory (1 byte address)
    private fun ZP0(): UByte {
        addrAbs = read(pc++).toUShort().and(255U)
        return 0U
    }

    // Zero Page X -> Zero page addressing plus X register value as offset
    private fun ZPX(): UByte {
        addrAbs = read(pc++).plus(regX).toUShort().and(255U)
        return 0U
    }

    // Zero Page Y -> Zero page addressing plus Y register value as offset
    private fun ZPY(): UByte {
        addrAbs = read(pc++).plus(regX).toUShort().and(255U)
        return 0U
    }

    // Relative -> Address to branch from must be within 1byte relative offset (branch instructions)
    private fun REL(): UByte {
        addrRel = read(pc++).toUShort()
        if(addrRel.and(128U) == 1U.toUShort()){
            addrRel = addrRel.or(256U.inv().toUShort())
        }
        return 0U
    }

    // Absolute -> Address using full 2 byte address
    private fun ABS(): UByte {
        addrAbs = (read(pc++).toUShort() shl 8).or(read(pc++).toUShort())
        return 0U
    }

    // Absolute X -> absolute addressing with x register value as offset
    private fun ABX(): UByte {
        val lo = read(pc++).toUShort()
        val hi = read(pc++).toUShort()
        addrAbs = (hi shl 8).or(lo).plus(regX).toUShort()
        return if (addrAbs.and(256u.inv().toUShort()) != (hi shl 8)) 1U else 0U
    }

    // Absolute Y -> absolute addressing with y register value as offset
    private fun ABY(): UByte {
        val lo = read(pc++).toUShort()
        val hi = read(pc++).toUShort()
        addrAbs = (hi shl 8).or(lo).plus(regY).toUShort()
        return if (addrAbs.and(256u.inv().toUShort()) != (hi shl 8)) 1U else 0U
    }

    // Indirect -> use 2 byte address to get actual 2 byte address
    private fun IND(): UByte {
        val lo = read(pc++).toUShort()
        val hi = read(pc++).toUShort()
        val ptr = (hi shl 8).or(lo)
        addrAbs = if(lo == 255U.toUShort()){ // simulate page boundary hardware bug
            (read( (ptr.and(256u.inv().toUShort())) shl 8)).or(read(ptr)).toUShort()
        } else {
            (read( (ptr.and(1u)) shl 8)).or(read(ptr)).toUShort()
        }
        return 0U
    }

    // Indirect X -> use 1 byte address plus x register value as offset for address on zero page
    private fun IZX(): UByte {
        val t = read(pc++).toUShort()
        val lo = read(t.plus(regX).and(255U).toUShort()).toUShort()
        val hi = read(t.plus(regX).plus(1U).and(255U).toUShort()).toUShort()
        addrAbs = (hi shl 8).or(lo)
        return 0U
    }

    // Indirect Y -> use 1 byte address plus y register value as offset for address on zero page
    private fun IZY(): UByte {
        val t = read(pc++).toUShort()
        val lo = read(t.and(255U)).toUShort()
        val hi = read(t.plus(1U).and(255U).toUShort()).toUShort()
        addrAbs = (hi shl 8).or(lo).plus(regY).toUShort()
        if(addrAbs.and(255U.inv().toUShort()) == (hi shl 8)){
            return 1U
        }
        return 0U
    }

}

private infix fun UShort.shl(i: Int): UShort = ((this.toInt()) shl i).toUShort()

