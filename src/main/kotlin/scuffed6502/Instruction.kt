package scuffed6502

// consider making this a data class
class Instruction (
        val name: String,
        var operate: (() -> Unit)? = null,
        var addrMode: (() -> Unit)? = null,
        var cycles: UByte = 0u
)
