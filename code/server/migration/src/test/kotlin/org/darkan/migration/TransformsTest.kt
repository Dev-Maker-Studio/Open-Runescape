package org.darkan.migration

import org.bson.Document
import org.darkan.core.model.ClanSetting
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransformsTest {

    @Test
    fun `account password history is renamed and reduced to argon2 strings`() {
        val doc = Document.parse("""{"username":"bob","email":"B@X.com","prevPasswords":["${'$'}argon2id${'$'}old",[1,2,3]],"social":{"friends":[],"ignores":[]}}""")
        val out = assertNotNull(AccountsTransform.apply(doc, Report("accounts")))
        assertFalse(out.containsKey("prevPasswords"))
        assertEquals(listOf("\$argon2id\$old"), out["previousPasswords"])
        assertEquals("b@x.com", out["email"])
    }

    @Test
    fun `account explicit nulls are unset and missing nested fields take the model defaults`() {
        val doc = Document.parse("""{"username":"nul","email":null,"displayName":null,"social":{"friends":[],"ignores":[],"friendsChat":{"friendsChatRanks":{}}}}""")
        val out = assertNotNull(AccountsTransform.apply(doc, Report("accounts")))
        assertFalse(out.containsKey("email"))
        assertFalse(out.containsKey("displayName"))
        val social = out.get("social", Document::class.java)
        assertEquals("help", social["currentFriendsChat"])
        assertEquals(0, social["status"])
        val fc = social.get("friendsChat", Document::class.java)
        assertEquals("OWNER", fc["rankToKick"])
        assertEquals(false, fc["coinshare"])
    }

    @Test
    fun `account null current friends chat is kept because it means not in a chat`() {
        val doc = Document.parse("""{"username":"quiet","email":"q@x.com","social":{"friends":[],"ignores":[],"currentFriendsChat":null}}""")
        val out = assertNotNull(AccountsTransform.apply(doc, Report("accounts")))
        val social = out.get("social", Document::class.java)
        assertTrue(social.containsKey("currentFriendsChat"))
        assertNull(social["currentFriendsChat"])
    }

    @Test
    fun `account without a username is dropped and reported`() {
        val report = Report("accounts")
        assertNull(AccountsTransform.apply(document("""{"email":"x@y.z"}"""), report))
        assertTrue(report.hasProblems)
    }

    @Test
    fun `clan settings move from client ids to enum names and doubles become integers`() {
        val doc = document("""{"name":"Cool","clanLeaderUsername":"bob","members":{"bob":{"rank":"OWNER","joinDate":1700000000000.0}},
            "settings":{"1":"our motto","14":3.0,"2":123456789012,"3":35651589},"vars":{"2811":2.0},"updateBlock":[0,1,2]}""")
        val out = assertNotNull(ClansTransform.apply(doc, Report("clans")))
        assertEquals("bob", out["leaderUsername"])
        assertFalse(out.containsKey("clanLeaderUsername"))
        assertFalse(out.containsKey("updateBlock"))
        val settings = out.get("settings", Document::class.java)
        assertEquals("our motto", settings[ClanSetting.MOTTO.name])
        assertEquals(3, settings[ClanSetting.MOTIF_TOP_ICON.name])
        assertEquals(123456789012L, settings[ClanSetting.FORUM_QFC.name])
        assertEquals(35651589, settings[ClanSetting.VAR_3.name])
        assertEquals("our motto", out["motto"])
        assertEquals(2, out.get("vars", Document::class.java)["2811"])
        val member = out.get("members", Document::class.java).get("bob", Document::class.java)
        assertEquals(1700000000000L, member["joinDate"])
    }

    @Test
    fun `clan settings with no modern definition are dropped and reported`() {
        val report = Report("clans")
        val out = assertNotNull(ClansTransform.apply(document("""{"name":"Cool","settings":{"999999":1}}"""), report))
        assertTrue(out.get("settings", Document::class.java).isEmpty())
        assertTrue(report.hasProblems)
    }

    @Test
    fun `player join date strings in every JDK flavour become real dates`() {
        for (text in listOf("Aug 27, 2024, 4:06:40 PM", "Aug 27, 2024 4:06:40 PM", "Aug 27, 2024, 4:06:40 PM", "Dec 30, 2023, 1:00:00 AM")) {
            val out = assertNotNull(PlayersTransform.apply(document("""{"username":"p","dateJoined":"$text"}"""), Report("players")))
            assertTrue(out["dateJoined"] is Date, "not converted: $text")
        }
        // The legacy reader's week-year bug must not be reproduced: Dec 30 2023 stays in 2023.
        val out = assertNotNull(PlayersTransform.apply(document("""{"username":"p","dateJoined":"Dec 30, 2023, 1:00:00 AM"}"""), Report("players")))
        assertTrue((out["dateJoined"] as Date).toInstant().toString().startsWith("2023-12-"))
    }

    @Test
    fun `unreadable player join date is unset but the save is kept`() {
        val report = Report("players")
        val out = assertNotNull(PlayersTransform.apply(document("""{"username":"p","dateJoined":"garbage"}"""), report))
        assertFalse(out.containsKey("dateJoined"))
        assertTrue(report.hasProblems)
    }

    @Test
    fun `log entry with an unreadable date is dropped`() {
        assertNull(LogsTransform.apply(document("""{"hash":1,"date":"garbage"}"""), Report("logs")))
        assertTrue(assertNotNull(LogsTransform.apply(document("""{"hash":1,"date":"Aug 27, 2024, 4:06:40 PM"}"""), Report("logs")))["date"] is Date)
    }

    private fun document(json: String) = Document.parse(json)
}
