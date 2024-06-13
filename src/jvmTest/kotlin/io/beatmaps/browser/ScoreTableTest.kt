package io.beatmaps.browser

import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import io.beatmaps.api.LeaderboardData
import io.beatmaps.api.LeaderboardScore
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Test
import java.lang.Integer.toHexString
import java.net.URI
import java.util.regex.Pattern
import kotlin.test.assertEquals

class ScoreTableTest : BrowserTestBase() {
    @Test
    fun `Can switch scoreboard after loading empty scores`() = bmTest {
        mock("/api/scores/**", LeaderboardData.serializer()) {
            val uri = URI(it.url())
            // val page = uri.path.split("/").last().toInt()
            val scoresaber = uri.query.endsWith("ScoreSaber")

            val scoresCount = when {
                scoresaber -> 0
                else -> 10
            }
            val scores = fixture.asSequence<LeaderboardScore>()
            LeaderboardData(
                false,
                "1",
                scores.take(scoresCount).toList(),
                mods = false,
                valid = true
            )
        }

        val mapId = transaction {
            val (uid, username) = createUser()
            val (mid, hash) = createMap(uid, true)

            mid
        }

        navigate("/maps/${toHexString(mapId)}")
        mapPage {
            // Scoresaber should be the default
            assertThat(tabs.scoresaber).hasClass(Pattern.compile("active"))
            scores.externalLink.waitFor()
            assertEquals(0, scores.count())

            // Switch to beatleader
            tabs.beatleader.click()
            assertThat(tabs.beatleader).hasClass(Pattern.compile("active"))

            // Wait for beatleader scores
            scores.externalLink.waitFor()
            assertEquals(20, scores.count())

            // Switch to reviews
            tabs.reviews.click()
            waitUntilGone(scores.elem)
        }
    }
}
