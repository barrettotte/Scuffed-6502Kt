package scuffed6502

// Read more here:  http://nesdev.com/6502.txt

enum class AddrMode{
    IMM, // Immediate             - next byte as value, point address to next byte in instruction
    ABS, // Absolute              - address using full 2 byte address
    ZP0, // Zero Page absolute    - address location using only first 256 bytes of memory (1 byte address)
    IMP, // Implied               - no operand addresses needed
    ACC, // Accumulator           - instruction operates on data in accumulator, no operands needed
    ABX, // Absolute X            - absolute addressing with x register value as offset
    ABY, // Absolute Y            - absolute addressing with y register value as offset
    ZPX, // Zero Page X           - zero page addressing plus x register value as offset
    ZPY, // Zero Page Y           - zero page addressing plus y register value as offset
    IND, // Indirect              - use 2 byte address to get actual 2 byte address
    IZX, // Pre-indexed indirect  - use 1 byte address plus x register value as offset for address on zero page
    IZY, // Post-indexed indirect - use 1 byte address plus y register value as offset for address on zero page
    REL  // Relative              - address to branch from must be within 1 byte relative offset (branch instructions)
}
