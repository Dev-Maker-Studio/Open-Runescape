package com.rs.game.content.quests.rovingelves

import com.rs.engine.book.Book
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import org.darkan.core.gameval.gen.Obj

private class CrystalOfSeren : Book("Crystal of Seren", """
Preface: This book is written to preserve the knowledge of crystal singing. It is by no means meant to cover the advanced concepts required to carry out a chant.

Chapter 1:

What is Crystal seed?

As you may know all crystal equipment is grown from a crystal seed. You may not know that all crystal seeds come from the same place; the ancestral home of Seren. Reading the 'Cerddi' tells us that crystal seed is sentient, although no one has ever been able converse with any crystal consciousness other than that of Seren. One of the ways we can see this consciousness in action is in the way that crystal equipment, when first used, becomes aligned to that person.

Chapter 2:

Crystal chanting.

As far as we can tell there have always been those with the ability to chant crystal, these tend to be from the Ithell family, but it has been known for these 'Crystal Chanters' to be born to other families. The thing that makes these 'Crystal Chanters' special is their ability to vocalise perfect pitches beyond that which humans can hear. The idea behind chanting is quite simple: different pitches can change the shape and size of any facet of the seed. This allows a practiced Chanter to make any number of shapes. It is also possible for particularly gifted individuals to imbue powers into these objects. The only problem with this process is that it is very costly and time consuming. The process can, however, be made more economical: the better the Chanter knows the person for whom they are creating items, the easier it is to make.

Chapter 3:

Crystal Properties.

Although crystal seeds are highly convenient they do have some drawbacks, the foremost being their brittleness. This will mean that any equipment made from a crystal seed will get weaker as it takes damage until eventually it reverts back to its original form, which can be reused to create any new crystal equipment. Unfortunately it is not possible to repair any Crystal items; it will need to return to seed before it can be made whole again.

Crystal Seed is one of our most valuable assets but with the loss of the 'World Gate' in the 2nd age access to this resource has been lost. Luckily there is no known way to destroy a crystal seed.
""".trimIndent())

@ServerStartupEvent
fun mapCrystalOfSeren() {
    onItemClick(Obj.roving_crystal_book, options = arrayOf("Read")) { (player) -> player.openBook(CrystalOfSeren()) }
}
