package com.rs.game.content.quests.legendsquest

import com.rs.engine.book.Book
import com.rs.plugin.annotations.ServerStartupEvent
import com.rs.plugin.kts.onItemClick
import org.darkan.core.gameval.gen.Obj

internal class LegendsBook(title: String, text: String) : Book(title, text)

@ServerStartupEvent
fun mapLegendsBooks() {
    onItemClick(Obj.scrawled_note1, options = arrayOf("Read")) { (player) ->
        player.openBook(LegendsBook("Scrawled note", """
Day 1... I have prepared the incantations, and will invoke the spirits of my ancestors to pay them homage.
Though I feel a strange presence in these caves, it is with the heart of a lion that I fight my fears and mark the magical octagram of binding.
Day 2... What have I done? My spirit is overthrown by feelings of fear and evil. I feel helpless, and weak... from my teachings, I know -
""".trimIndent()))
    }
    onItemClick(Obj.scrawled_note2, options = arrayOf("Read")) { (player) ->
        player.openBook(LegendsBook("A scribbled note", """
The spirit of an ancient one lurks deep within my soul... I am too weak to cast the curse myself and fight the beast within.
Day 3 ...my last hope is that someone will read this and aid me... I am undone and I fear the worst...
""".trimIndent()))
    }
    onItemClick(Obj.scrawled_note3, options = arrayOf("Read")) { (player) ->
        player.openBook(LegendsBook("Scrumpled note", "Day 4... is it Day 4? I feel so confused... Day 5... A wizened charm will release me, but never magic that would harm..."))
    }
    onItemClick(Obj.shamans_tome, options = arrayOf("Read")) { (player) ->
        player.openBook(LegendsBook("Shaman's tome", "...scattered are my hopes that I will ever be released from this flaming Octagram. It is the only thing that will contain this beast within. Although its grip over me is weakened with magic, it is hopeless to know if a saviour would guess this. I am doomed..."))
    }
}
