package scuffed6502

data class Instruction(
    val mnemonic: String = "NOP",
    val mode: Int = 1,
    val size: Int = 0,
    val cycles: Int = 0,
    val pageCycles: Int = 0
)
