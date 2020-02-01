package org.github.jczuchnowski.zio.workshop.server

import scala.io.Source
import java.io.File
import zio.Chunk
import java.nio.ByteBuffer

case class Pokemon(name: String, hp: Int, attack: Int, defense: Int)

object PokemonService {

  val file = Source.fromFile(new File("pokemon.csv"))

  val header :: lines = file.getLines().toList

  val pokemons = lines.map(_.split(",")).map { line =>
    val name = line(1)
    val type1 = line(2)
    val type2 = Option(line(3))
    val total = line(4).toLong
    val hp = line(5).toInt
    val attack = line(6).toInt
    val defense = line(7).toInt
    val gen = line(11).toInt
    val leg = line(12).toBoolean

    Pokemon(name, hp, attack, defense)
  }
}

object PokemonEncoder {

  val nul: Array[Byte] = ByteBuffer.allocate(1).put(0.toByte).array()

  val `type`: Byte = 'P'.toByte

  def toBytes(pokemon: Pokemon): Chunk[Byte] = {
    val name = pokemon.name.toCharArray().map(_.toByte)

    val nameLen = name.length + 1
    val len: Array[Byte] = ByteBuffer.allocate(4).putInt(nameLen + 12).array()

    val hp: Array[Byte] = ByteBuffer.allocate(4).putInt(pokemon.hp).array()
    val attack: Array[Byte] = ByteBuffer.allocate(4).putInt(pokemon.attack).array()
    val defense: Array[Byte] = ByteBuffer.allocate(4).putInt(pokemon.defense).array()

    Chunk.fromArray((`type` +: len) ++ name ++ nul ++ hp ++ attack ++ defense)
  }

}