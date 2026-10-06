# Java backend architecture notes

Products live in MongoDB; users and orders live in PostgreSQL. Checkout snapshots titles and prices inside the SQL transaction. Later catalog edits do not change receipts. Money is calculated with BigDecimal and stored as NUMERIC; catalog API prices accept two decimal places.

After SQL commit, the Java API publishes the order ID to a durable RabbitMQ queue and waits for publisher confirmation. If publication fails, the order remains saved and the response reports `queue_unavailable`. A Java consumer fetches current SQL state and writes an Elasticsearch document using external version checks. Stale deliveries cannot replace a newer indexed version. This replaces the previous Python worker entirely.

Transient indexing errors retry five times with exponential backoff; exhausted messages go to the durable failed queue. An admin can call `POST /api/search/reindex` to publish all current orders again after recovery. Search uses only Elasticsearch and returns an explicit unavailable error when the search service fails.

This is queued dual-write, not an outbox: the SQL commit and queue publish are separate operations. There is a failure gap and no exactly-once checkout guarantee. Data volumes and existing schema are preserved, with idempotent password/role columns added at startup. Existing legacy users must not acquire a password through unauthenticated account claiming.

Admin signup uses a private configured invitation. Customer/admin permissions and order ownership are enforced on the server. Sessions use HttpOnly cookies and CSRF protection. Password recovery, email verification and rate limiting remain outside the current local prototype.
