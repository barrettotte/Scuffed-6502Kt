package scuffed6502

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class Instruction (
        val mnemonic: String = "",
        val impl: String = "",
        val addrMode: String = "",
        val cycles: Int = 0
)
