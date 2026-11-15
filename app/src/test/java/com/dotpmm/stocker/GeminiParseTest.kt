package com.dotpmm.stocker

import com.dotpmm.stocker.domain.ai.Gemini
import com.dotpmm.stocker.domain.ai.GeminiException
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class GeminiParseTest {
    @Test fun parsesPlainJson() {
        val raw = """{"stance":"Positive","summary":"Good quarter.","drivers":[{"text":"Profit up","refs":[1,2]}],
            "risks":[{"text":"Margins","refs":[3]}],"upcoming":[]}"""
        val s = Gemini.parse(raw, "m", listOf("a", "b", "c"))
        assertEquals("positive", s.stance)
        assertEquals(1, s.drivers.size)
        assertEquals(listOf(1, 2), s.drivers[0].refs)
        assertEquals(0, s.upcoming.size)
    }

    @Test fun parsesFencedJson() {
        val s = Gemini.parse("```json\n{\"stance\":\"mixed\",\"summary\":\"x\"}\n```", "m", emptyList())
        assertEquals("mixed", s.stance)
    }

    @Test fun rejectsGarbage() {
        try {
            Gemini.parse("not json", "m", emptyList())
            fail("expected GeminiException")
        } catch (e: GeminiException) {
            // expected
        }
    }
}
