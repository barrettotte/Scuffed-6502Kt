package scuffed6502

data class Instruction(
    val id: Int = 0,
    val mnemonic: String = "NOP",
    val mode: String = "IMM",
    val size: Int = 0,
    val cycles: Int = 0
)
