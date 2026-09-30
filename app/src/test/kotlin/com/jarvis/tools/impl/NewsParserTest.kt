package com.jarvis.tools.impl

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NewsParserTest {
    @Test
    fun parses_and_deduplicates_gdelt_articles() {
        val json = """
            {
              "articles": [
                {
                  "title": "Headline A",
                  "url": "https://example.com/a",
                  "domain": "example.com",
                  "seendate": "20260930T100000Z",
                  "language": "English",
                  "sourcecountry": "United States"
                },
                {
                  "title": "Headline A duplicate",
                  "url": "https://example.com/a",
                  "domain": "example.com"
                },
                {
                  "title": "Headline B",
                  "url": "https://example.org/b",
                  "domain": "example.org",
                  "seendate": "20260930T101500Z"
                }
              ]
            }
        """.trimIndent()

        val articles = NewsParser.parse(json, 10)

        assertThat(articles).hasSize(2)
        assertThat(articles[0].title).isEqualTo("Headline A")
        assertThat(articles[1].url).isEqualTo("https://example.org/b")
    }

    @Test
    fun malformed_json_returns_empty() {
        assertThat(NewsParser.parse("not-json", 10)).isEmpty()
    }
}
