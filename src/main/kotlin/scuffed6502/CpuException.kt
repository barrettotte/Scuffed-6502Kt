package scuffed6502

import java.lang.Exception

class CpuException(msg: String, opcode: Int, status: Int): Exception(
    "$msg\nopcode: 0x${opcode.toString(16).padStart(2, '0')}     " +
        "status: ${status.toString(2).padStart(8,'0')} (NVUDBIZC)")
