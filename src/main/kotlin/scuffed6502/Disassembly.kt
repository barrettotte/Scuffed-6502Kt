package scuffed6502

data class Disassembly(
    var address: Int,
    var assembly: String,
    var instruction: Instruction,
    var hex: String
){
    override fun toString(): String{
        return "$assembly {${instruction.mode.name}}    {${"%02X".format(instruction.opcode)}}    "  +
            "{${instruction.cycles}}        $hex"
    }
}
