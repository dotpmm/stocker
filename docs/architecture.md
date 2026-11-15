# Architecture (draft)

Fully on-device. No server, no accounts. The Gemini key is stored on the device only.

## Pipeline

```
sources (RSS, Google News queries, Yahoo news, filings best-effort)
  -> fetch (conditional GET, per-domain rate limit, health record)
  -> normalise (title, summary, publisher, URL, published time)
  -> dedupe / syndication clustering (fuzzy title ratio >= 90 within 48 h)
  -> entity linking (alias table from NSE EQUITY_L.csv, aliases, exclusion phrases; title match = highest confidence)
  -> rule-based sentiment (finance lexicon) + event tags (results, guidance, upgrade/downgrade, orders, M&A,
     regulatory, dividend, management change)
  -> impact score = source credibility x recency x match strength x confirmation, negativity weighted up
  -> per-stock roll-up (tone, event list, volume spikes) -> Room database -> UI
```

## Indirect-impact news

Topic profiles per stock: peers, key executives, regulators, markets/countries, themes. An article that matches a
profile but not the company name is attached as "related" with a lower match strength. AI-generated topic profiles
can come later.

## AI summary

No vector database. Take the ~20-30 most recent relevant articles for a stock, put titles, summaries and sources
in one Gemini prompt, and request structured JSON: stance, drivers, risks, upcoming events, and the article IDs
that support each point. Cache by hash of the article set so repeat taps are free. Show the rule-based tone next
to the AI view. Always label as not financial advice.

## Scheduling

WorkManager periodic work every 12 hours with network constraint, plus a manual "refresh now" that runs the same
pipeline immediately. Background work only fetches and processes; notifications are optional and off by default.

## Modules (proposed)

- `data/sources`: fetchers per source type, health tracking
- `data/db`: Room entities (stock, article, cluster, event, source_health)
- `domain/nlp`: dedupe, entity linking, sentiment, event tagging, scoring
- `domain/ai`: Gemini client, prompt builder, cache
- `ui`: watchlist, stock detail, reader, events, settings, source health

## As built (v1.5)

- `data/net`: `Http` (per-host gate), `FeedParser` (RSS/Atom), `Sources` (feed list, Google News URLs), `Yahoo`
  (search and charts), `ArticleReader` (reader-mode extraction).
- `data`: `Ingest` (rule-based pipeline), `StockerStore` (refresh engine and queries), `AppPrefs`, `Popular`.
- `data/db`: `StockerDb` (articles, article_stock, events, quotes, chart_cache, ai_cache, source_health).
- `domain/nlp`: `Sentiment`, `EventTagger`, `EventExtractor`, `Linker`, `Credibility`, `Impact`, `TextUtil`.
- `domain/ai`: `Gemini` (model discovery, prompt, parsing).
- `work`: `RefreshWorker` and `RefreshScheduler` (12 hours, network required).
- Duplicates: headlines are tokenised; two stories within 48 hours with at least 80 percent containment and 50 percent
  Jaccard overlap share a cluster, and the best-credibility copy represents it.
- Events come from dates found in news text (not from exchange filings yet); an event attaches to the most strongly
  linked company.
