package scuffed6502

data class Instruction(
    val mnemonic: String = "???",
    val opcode: Int = "EA".toInt(16),
    val size: Int = 1,
    val cycles: Int = 2,
    val mode: AddrMode = AddrMode.IMP
)
