package scuffed6502

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


    private lateinit var bus: Bus
    private var regAcc: UByte = 0u
    private var regX: UByte = 0u
    private var regY: UByte = 0u
    private var stkPtr: UByte = 0u
    private var pc: UShort = 0u
    private var regStatus: UByte = 0u

    private var fetched: UByte = 0u

    // In the 6502, branching can only occur within a certain distance of originating call
    private var addrAbs: UShort = 0u
    private var addrRel: UShort = 0u

    private var opcode: UByte = 0u
    private var cycles: UByte = 0u

    private var lookup: MutableList<Instruction> = mutableListOf() // hold 16x16 table of instructions



    override fun connectToBus(bus: Bus){ this.bus = bus }

    override fun read(addr: UShort): UByte? = this.bus.readFromRAM(addr)

    override fun write(addr: UShort, data: UByte): Boolean = this.bus.writeToRAM(addr, data)


    // Access status register
    fun getFlag(f: FLAGS) = 1
    fun setFlag(f: FLAGS, v: Boolean) = 1


    // Perform a clock cycle
    fun tickClock() = 1

    fun reset() = 1

    // Execute instruction at specific location
    fun interruptReq() = 1

    // Execute instruction at specific location, cannot be disabled
    fun nmInterruptReq() = 1

    fun fetch() = 1

    // Addressing Modes - access data in memory (direct/indirect)
    fun IMP() = 1
    fun IMM() = 1
    fun ZP0() = 1
    fun ZPX() = 1
    fun ZPY() = 1
    fun REL() = 1
    fun ABS() = 1
    fun ABX() = 1
    fun ABY() = 1
    fun IND() = 1
    fun IZX() = 1
    fun IZY() = 1


    // Opcodes
    fun ADC() = 1
    fun AND() = 1
    fun ASL() = 1
    fun BCC() = 1
    fun BCS() = 1
    fun BEQ() = 1
    fun BIT() = 1
    fun BMI() = 1
    fun BNE() = 1
    fun BPL() = 1
    fun BRK() = 1
    fun BVC() = 1
    fun BVS() = 1
    fun CLC() = 1
    fun CLD() = 1
    fun CLI() = 1
    fun CLV() = 1
    fun CMP() = 1
    fun CPX() = 1
    fun CPY() = 1
    fun DEC() = 1
    fun DEX() = 1
    fun DEY() = 1
    fun EOR() = 1
    fun INC() = 1
    fun INX() = 1
    fun INY() = 1
    fun JMP() = 1
    fun JSR() = 1
    fun LDA() = 1
    fun LDX() = 1
    fun LDY() = 1
    fun LSR() = 1
    fun NOP() = 1
    fun ORA() = 1
    fun PHA() = 1
    fun PHP() = 1
    fun PLA() = 1
    fun PLP() = 1
    fun ROL() = 1
    fun ROR() = 1
    fun RTI() = 1
    fun RTS() = 1
    fun SBC() = 1
    fun SEC() = 1
    fun SED() = 1
    fun SEI() = 1
    fun STA() = 1
    fun STX() = 1
    fun STY() = 1
    fun TAX() = 1
    fun TAY() = 1
    fun TSX() = 1
    fun TXA() = 1
    fun TXS() = 1
    fun TYA() = 1

    // illegal opcode
    fun XXX() = 1

    init {
        val lookupJson = this.javaClass.getResourceAsStream("/Lookup.json")
    }
}