package com.dotpmm.stocker

import com.dotpmm.stocker.domain.model.Market
import com.dotpmm.stocker.domain.model.Tone
import com.dotpmm.stocker.domain.model.WatchStock
import com.dotpmm.stocker.domain.nlp.EventExtractor
import com.dotpmm.stocker.domain.nlp.EventTagger
import com.dotpmm.stocker.domain.nlp.Linker
import com.dotpmm.stocker.domain.nlp.Names
import com.dotpmm.stocker.domain.nlp.Sentiment
import com.dotpmm.stocker.domain.nlp.TextUtil
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NlpTest {
    private fun stock(ticker: String, name: String, vararg aliases: String) = WatchStock(
        ticker, name, Market.India, "$ticker.NS", "NSE", aliases.toList(),
    )

    private val linker = Linker(
        listOf(
            stock("RELIANCE", "Reliance Industries Limited", "Reliance", "RIL"),
            stock("TCS", "Tata Consultancy Services"),
            stock("LT", "Larsen & Toubro", "L&T"),
            stock("ITC", "ITC"),
            stock("INFY", "Infosys"),
        ),
    )

    // ---- sentiment ----

    @Test fun positiveHeadline() =
        assertEquals(Tone.Positive, Sentiment.analyze("Reliance shares surge 5% after record quarterly profit", ""))

    @Test fun negativeHeadline() =
        assertEquals(Tone.Negative, Sentiment.analyze("Infosys shares plunge as regulator opens probe", ""))

    @Test fun neutralHeadline() =
        assertEquals(Tone.Neutral, Sentiment.analyze("HDFC Bank announces date for annual general meeting", ""))

    @Test fun marketFall() =
        assertEquals(Tone.Negative, Sentiment.analyze("Sensex falls 300 points as IT stocks drag", ""))

    @Test fun negationFlips() =
        assertEquals(Tone.Positive, Sentiment.analyze("Company reports no loss in third quarter", ""))

    @Test fun phraseAllTimeHigh() =
        assertEquals(Tone.Positive, Sentiment.analyze("Titan stock hits all-time high", ""))

    // ---- linking ----

    @Test fun titleMatchIsStrongest() {
        val links = linker.link("Reliance Industries Q2 profit rises 12%", "")
        assertEquals(1.0, links["RELIANCE"]!!, 0.0)
        assertFalse(links.containsKey("TCS"))
    }

    @Test fun summaryOnlyIsWeaker() {
        val links = linker.link("Markets close higher", "Gains were led by Tata Consultancy Services and others")
        assertEquals(0.6, links["TCS"]!!, 0.0)
    }

    @Test fun commonWordDoesNotLink() {
        val links = linker.link("Heavy reliance on imports hurts exporters", "")
        assertFalse(links.containsKey("RELIANCE"))
    }

    @Test fun tickerAndAmpersandAliases() {
        assertTrue(linker.link("L&T bags large orders from Gulf", "").containsKey("LT"))
        assertTrue(linker.link("INFY and TCS lead IT rally", "").containsKey("INFY"))
        assertTrue(linker.link("ITC hotels demerger update", "").containsKey("ITC"))
    }

    @Test fun noFalseLinkInsideWords() {
        assertTrue(linker.link("Switch to citcom plan", "").isEmpty())
    }

    // ---- tags and events ----

    @Test fun tags() {
        assertEquals("Rating", EventTagger.tag("Brokerage upgrades Infosys with higher target price", ""))
        assertEquals("Results", EventTagger.tag("Infosys Q2 net profit rises", ""))
        assertEquals("Dividend", EventTagger.tag("TCS announces interim dividend", ""))
        assertEquals("News", EventTagger.tag("Infosys opens new campus", ""))
    }

    private fun millis(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli() + 6 * 3_600_000L

    @Test fun boardMeetingDate() {
        val e = EventExtractor.extract("XYZ board meeting on 20 November to consider Q2 results", "", millis(2026, 11, 10))
        assertNotNull(e)
        assertEquals("Board meeting", e!!.first)
        assertEquals(LocalDate.of(2026, 11, 20).toEpochDay(), e.second)
    }

    @Test fun resultsDateMonthFirst() {
        val e = EventExtractor.extract("ABC to announce Q2 results on Nov 14", "", millis(2026, 11, 10))
        assertEquals("Results", e!!.first)
        assertEquals(LocalDate.of(2026, 11, 14).toEpochDay(), e.second)
    }

    @Test fun pastResultsAreIgnored() {
        assertNull(EventExtractor.extract("ABC reported Q2 results on Nov 5", "", millis(2026, 11, 10)))
    }

    @Test fun yearRollover() {
        val e = EventExtractor.extract("ABC record date 5 January for dividend", "", millis(2026, 12, 20))
        assertEquals(LocalDate.of(2027, 1, 5).toEpochDay(), e!!.second)
    }

    // ---- text and dedupe ----

    @Test fun stripsHtmlAndEntities() {
        assertEquals("Profit & loss <up>", TextUtil.stripHtml("<p>Profit &amp; loss&nbsp;&lt;up&gt;</p>"))
    }

    @Test fun canonicalUrlDropsTracking() {
        assertEquals(
            "https://example.com/a?id=5",
            TextUtil.canonicalUrl("https://example.com/a?utm_source=x&id=5&fbclid=z#top"),
        )
    }

    private fun tok(title: String) = TextUtil.tokens(TextUtil.normalizeTitle(title))

    @Test fun syndicatedCopiesCluster() {
        val a = tok("Reliance Q2 profit rises 12% to Rs 20,000 crore")
        val b = tok("Reliance Industries Q2 profit up 12% at Rs 20,000 crore")
        assertTrue(TextUtil.similarity(a, b) > 0.0)
    }

    @Test fun differentStoriesDoNotCluster() {
        val a = tok("Reliance Q2 profit rises 12% to Rs 20,000 crore")
        val b = tok("Reliance announces new retail store expansion across southern states")
        assertEquals(0.0, TextUtil.similarity(a, b), 0.0)
    }

    @Test fun cleansCompanyNames() {
        assertEquals("Reliance Industries", Names.clean("Reliance Industries Limited"))
        assertEquals("Apple", Names.clean("Apple Inc."))
        assertEquals("Infosys", Names.clean("Infosys Ltd"))
    }
}
