package com.batuhankeskn0.kaimon

import com.batuhankeskn0.kaimon.data.local.UserProgressEntity
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.AIError
import com.batuhankeskn0.kaimon.data.service.AIService
import com.batuhankeskn0.kaimon.data.service.AiHttpResponse
import com.batuhankeskn0.kaimon.data.service.AiHttpTransport
import com.batuhankeskn0.kaimon.data.service.InMemoryReminderStorage
import com.batuhankeskn0.kaimon.data.service.NotificationManager
import com.batuhankeskn0.kaimon.ui.screens.chat.ChatViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.util.TimeZone

class Phase9ScenariosAiNotificationsTest {

    private class FakeTransport(
        private val responses: MutableList<AiHttpResponse> = mutableListOf(),
        private val throwIo: Boolean = false
    ) : AiHttpTransport {
        val urls = mutableListOf<String>()
        override suspend fun postJson(url: String, headers: Map<String, String>, bodyJson: String): AiHttpResponse {
            urls += url
            if (throwIo) throw IOException("offline")
            return responses.removeAt(0)
        }
    }

    private fun geminiOk(text: String) =
        AiHttpResponse(200, """{"candidates":[{"content":{"parts":[{"text":"$text"}]}}]}""")

    private fun service(t: AiHttpTransport, provider: String = "gemini") =
        AIService(t, { "test-key" }, { provider }, retryDelayMs = 0L)

    @Test
    fun `scenarios load with level labels`() {
        val scenarios = ContentStore().loadScenarios()
        assertTrue(scenarios.isNotEmpty())
        assertTrue(scenarios.all { it.level.isNotBlank() && it.steps.isNotEmpty() })
    }

    @Test
    fun `gemini success appends history and reset restores system prompt`() = runTest {
        val svc = service(FakeTransport(mutableListOf(geminiOk("こんにちは (Merhaba)"))))
        assertEquals("こんにちは (Merhaba)", svc.sendMessage("selam"))
        assertEquals(3, svc.getMessageHistory().size)
        svc.resetConversation()
        assertEquals(1, svc.getMessageHistory().size)
    }

    @Test
    fun `gemini falls back to next model on 503 and 429`() = runTest {
        val t = FakeTransport(
            mutableListOf(AiHttpResponse(503, "{}"), AiHttpResponse(429, "{}"), geminiOk("ok"))
        )
        assertEquals("ok", service(t).sendMessage("x"))
        assertEquals(3, t.urls.size)
    }

    @Test
    fun `persistent 429 yields rate limit message and rolls back history`() = runTest {
        val t = FakeTransport(MutableList(AIService.DEFAULT_GEMINI_MODELS.size) { AiHttpResponse(429, "{}") })
        val svc = service(t)
        try {
            svc.sendMessage("x")
            fail()
        } catch (e: AIError) {
            assertEquals(AIService.RATE_LIMIT_ERROR_MESSAGE, e.message)
        }
        assertEquals(1, svc.getMessageHistory().size)
    }

    @Test
    fun `offline yields friendly message`() = runTest {
        try {
            service(FakeTransport(throwIo = true)).sendMessage("x")
            fail()
        } catch (e: AIError) {
            assertEquals(AIService.OFFLINE_ERROR_MESSAGE, e.message)
        }
    }

    @Test
    fun `openai provider parses choices`() = runTest {
        val t = FakeTransport(
            mutableListOf(AiHttpResponse(200, """{"choices":[{"message":{"content":"hi"}}]}"""))
        )
        assertEquals("hi", service(t, "openai").sendMessage("x"))
        assertEquals(AIService.OPENAI_ENDPOINT, t.urls.single())
    }

    @Test
    fun `parseBotMessage splits japanese and translation`() {
        val p = ChatViewModel.parseBotMessage("こんにちは！ (Merhaba!)")
        assertEquals("こんにちは！", p.japanese)
        assertEquals("Merhaba!", p.translation)
        assertNull(ChatViewModel.parseBotMessage("merhaba").translation)
    }

    @Test
    fun `reminder content matches iOS texts`() {
        val streak = NotificationManager.buildReminderContent(5)
        assertEquals("Serini Kaybetme! 🔥", streak.title)
        assertTrue(streak.body.contains("Tam 5 gündür"))
        assertEquals("Çalışma Zamanı!", NotificationManager.buildReminderContent(0).title)
    }

    @Test
    fun `daily reminder schedules next trigger and streak update refreshes text`() {
        val storage = InMemoryReminderStorage()
        val utc = TimeZone.getTimeZone("UTC")
        val now = 1_700_000_000_000L
        val mgr = NotificationManager(storage, { now }, { utc }, initialAuthorized = true)
        storage.isDailyReminderEnabled = true
        mgr.scheduleDailyReminder(20, 0, activeStreak = 0)
        val first = mgr.lastScheduledReminder
        assertNotNull(first)
        assertTrue(first!!.triggerAtMillis > now)
        assertEquals("Çalışma Zamanı!", first.title)

        mgr.updateDailyReminderStreak(7)
        assertTrue(mgr.lastScheduledReminder!!.body.contains("Tam 7 gündür"))
        assertEquals(20, mgr.lastScheduledReminder!!.hour)

        mgr.cancelDailyReminder()
        assertNull(mgr.lastScheduledReminder)
    }

    @Test
    fun `recordStudySession pushes active streak to notification manager`() {
        val storage = InMemoryReminderStorage()
        val mgr = NotificationManager(storage, initialAuthorized = true)
        NotificationManager.setSharedForTesting(mgr)
        storage.isDailyReminderEnabled = true
        UserProgressEntity().recordStudySession()
        assertEquals(1, mgr.lastScheduledReminder?.activeStreak)
        assertFalse(mgr.lastScheduledReminder!!.body.isEmpty())
    }
}
