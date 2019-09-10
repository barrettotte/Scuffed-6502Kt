package scuffed6502

class Instruction (
        val name: String,
        var operate: (() -> Unit)? = null,
        var addrMode: (() -> Unit)? = null,
        var cycles: UByte = 0u
)
