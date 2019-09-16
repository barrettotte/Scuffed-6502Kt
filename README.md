# Scuffed 6502 Kt


I've been planning on learning kotlin for awhile 
and I've also been planning on making a 6502 emulator, so why not combine both (terrible idea brother).


The inspiration for this little project came from stumbling across OneLoneCoder's series on building an NES emulator.
He has an awesome video [here](https://www.youtube.com/watch?v=8XmxKPJDGU0) on putting together a 6502 CPU in C++,
so I followed along in Kotlin and made a couple changes to the design (namely more OOP, JVMish things, and more modular design)


It was a bit lame of me to not go and figure everything out myself, but the ultimate goal was to learn kotlin and a bit of 6502 architecture


## Design Choices
* Honestly a lot of the OOP I did was not needed, but I wanted to get more hands on Kotlin OOP experience in a more fun way
* I used Kotlin's unsigned numerics to get some free type/range checking out of the box (ex: addr cannot be < 0)
* I changed the instruction lookup table to be JSON and kotlin metaprogramming driven


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
* OneLoneCoder inspiration
  * Video https://www.youtube.com/watch?v=8XmxKPJDGU0
  * Repository https://github.com/OneLoneCoder/olcNES
* Datasheet http://archive.6502.org/datasheets/rockwell_r650x_r651x.pdf
* 6502 Instruction set https://www.masswerk.at/6502/6502_instruction_set.html
* 6502 Addressing modes http://www.obelisk.me.uk/6502/index.html
* 6502 Assembler https://www.masswerk.at/6502/assembler.html
* http://www.6502.org/
  * http://www.6502.org/tutorials/
  * http://www.6502.org/users/andre/65k/af65002/af65002arch.html 
* Kotlin bitwise operators https://www.programiz.com/kotlin-programming/bitwise