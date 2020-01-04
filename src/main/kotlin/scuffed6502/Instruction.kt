package scuffed6502

data class Instruction(
    val mnemonic: String = "???",
    val opcode: Int = "EA".toInt(16),
    val mode: AddrMode = AddrMode.IMP,
    val cycles: Int = 2
)
