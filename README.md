# Scuffed 6502 Kt

I've been planning on learning kotlin for awhile since I'm already pretty familiar with Java and Groovy.
So, why not make something interesting.


## Commands
* ```./run.sh``` or ```gradlew run```


## Kotlin Notes
* data types
  * kotlin.UByte: an **unsigned 8-bit integer**, ranges from 0 to 255
  * kotlin.UShort: an **unsigned 16-bit integer**, ranges from 0 to 65535
  * kotlin.UInt: an unsigned 32-bit integer, ranges from 0 to 2^32 - 1
  * kotlin.ULong: an unsigned 64-bit integer, ranges from 0 to 2^64 - 1
* Hex literal conversions
  * 0x80 - 128U
  * 0x00FF - 255U
  * 0xFF00 - 65280U


## References
* Datasheet http://archive.6502.org/datasheets/rockwell_r650x_r651x.pdf
* 6502 Instruction set https://www.masswerk.at/6502/6502_instruction_set.html
* 6502 Addressing modes http://www.obelisk.me.uk/6502/index.html
* 6502 Assembler https://www.masswerk.at/6502/assembler.html
* http://www.6502.org/
  * http://www.6502.org/tutorials/
  * http://www.6502.org/users/andre/65k/af65002/af65002arch.html 
* Kotlin bitwise operators https://www.programiz.com/kotlin-programming/bitwise

