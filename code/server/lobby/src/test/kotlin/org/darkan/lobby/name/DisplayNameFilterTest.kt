package org.darkan.lobby.name

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class DisplayNameFilterTest {

    private val cleanNames = listOf(
        "Zezima", "Woox", "Lynx Titan", "Dark Scape", "Mister Bean", "Bean Counter",
        "Assassin", "Grand Assassin", "Bass Player", "Grass Roots", "Class Act", "Pass Master",
        "Analyst", "Canal Boat", "Banal", "Uranus", "Manuscript",
        "Scunthorpe", "Penistone", "Shiitake", "Therapist", "Therapy Dog",
        "Raccoon", "Cocoon", "Tycoon", "Cooney",
        "Cocktail", "Peacock", "Cockney", "Cockatrice", "Dickens", "Dickinson",
        "Spice Girl", "Suspicion", "Pakistan",
        "Titan", "Titanium", "Buttons", "Butter", "Button Masher",
        "Cumin Seed", "Cumulus", "Accumulate", "Circumvent", "Documenta",
        "Grape Juice", "Scraping By", "Draping", "Fire Retardant", "Custard", "Mustard",
        "Hello There", "Shell Shock", "Hell Raiser", "Sheltie", "Michelle",
        "Arsenal Fan", "Arsenic",
        "Sass Master", "Massive", "Passive", "Compass", "Embassy",
        "Magic Man", "Mage Hunter", "Iron Man", "Pure Skiller", "Zamorak Wolf",
        "Fisher King", "Fish Cakes", "Fiction", "Friction",
        "Nazir", "Nazionale", "Isis Temple", "Crisis Mode", "Basis Point",
        "Carnivore", "Herbivore", "Omnivore",
        "Scatter Gun", "Scattered", "Scattershot",
        "Modern Art", "Model Maker", "Module", "Modest Mouse", "Ownership", "Downer",
        "Lollipop", "Holiday", "Melodic",
        "Piston", "Turtle", "Turbine", "Portland", "Import Man",
        "Fagus Tree", "Fagotto", "Wig Maker", "Bigger Boat", "Digger",
        "Assist", "Asset", "Assess", "Glass Half", "Brass Monkey",
        "Hoe Down", "Fanny Pack",
        "Cli Bot 455", "Player 455", "Zezima 1337", "Bot 480", "Bot 717", "Swanky Mongol",
    )

    private val profaneNames = listOf(
        "Fuck You", "FuCk", "F u c k", "F_u_c_k", "Fuuuck", "Phuck Off", "Fuck3r",
        "Shithead", "Sh1t", "Bull5hit",
        "Cunt Face", "C u n t", "Bitch Boy", "B1tch", "Bastard", "Moby Dick",
        "Nigger", "N1gger", "Niiigggerrr", "Nigga", "N i g g a", "Sandnigger", "Wigger",
        "Faggot", "F4gg0t", "Fag", "Fags", "Fagg0tt",
        "Futanari", "Futa", "Hentai", "Ahegao", "Lolicon", "Shotacon", "Lolita",
        "Bestiality", "Zoophile", "Necrophile", "Coprophile", "Yiffing",
        "Incest", "Molester", "Pedophile", "Paedophile", "Pedo", "Jailbait",
        "Rapist", "The Rapist", "Raping", "Rape", "Gangrape",
        "Cumshot", "Creampie", "Gangbang", "Bukkake", "Deepthroat", "Felching", "Queef",
        "Dildo", "Buttplug", "Cocksucker", "Dickhead", "Jerkoff", "Wanker",
        "Clitoris", "Scrotum", "Smegma", "Ejaculate", "Masturbate", "Prolapse",
        "Pussy Cat", "Titties", "Boobies", "Pornhub", "Porn Star", "Onlyfans",
        "Retarded", "Mongoloid", "Tranny", "Shemale", "Ladyboy", "Spastic", "Midget",
        "Negros", "Kike", "Spic", "Gook", "Paki", "Dago", "Wop", "Beaner", "Wetback", "Towelhead",
        "Hitler", "Nazi", "Sieg Heil", "Swastika", "Heil", "Kkk", "Gestapo",
        "Anal og", "Anal", "Anus", "Ass", "Arse", "Cum", "Jizz", "Twat", "Slut", "Scat", "Loli",
    )

    @Test
    fun `ordinary names are never flagged`() {
        for (name in cleanNames) assertNull(DisplayNameFilter.match(name), "false positive on '$name'")
    }

    @Test
    fun `profanity and its common disguises are flagged`() {
        for (name in profaneNames) assertNotNull(DisplayNameFilter.match(name), "missed '$name'")
    }

    @Test
    fun `staff names are reserved`() {
        assertEquals(DisplayNameVerdict.RESERVED, DisplayNameFilter.match("Mod Trent")?.verdict)
        assertEquals(DisplayNameVerdict.RESERVED, DisplayNameFilter.match("Jagex")?.verdict)
        assertNull(DisplayNameFilter.match("Modern Warfare"))
    }

    @Test
    fun `a word of digits is a number, not leetspeak`() {
        assertNull(DisplayNameFilter.match("455"))
        assertNull(DisplayNameFilter.match("Iron Man 455"))
        assertNotNull(DisplayNameFilter.match("A55hole"))
        assertNotNull(DisplayNameFilter.match("455hole"))
    }

    @Test
    fun `a squeezed match never collapses a shorter clean word into a term`() {
        assertNull(DisplayNameFilter.match("As Above"))
        assertNull(DisplayNameFilter.match("Ba Na Na"))
        assertNotNull(DisplayNameFilter.match("Niggger"))
    }
}
