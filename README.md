# BuyMore

A Vue storefront and admin dashboard backed by Java 21 / Spring Boot. Landing, customer login/registration, and admin login/registration are separate pages.

## Run locally

Requirements: Docker Engine with Compose v2. For host development, also install Java 21, Maven and Node.js.

```bash
cp .env.example .env
# Set ADMIN_INVITATION_CODE in .env to a long private value if you need admin registration.
docker compose up --build -d
```

Open http://localhost:15174. Register a customer at `/register`, or register an admin at `/admin/register` using the configured invitation. No default passwords or admin accounts are created. A blank invitation setting disables admin registration. The configured code is reusable; rotate it after onboarding your team. Existing legacy users have no password and cannot log in; use a new email until a verified account recovery flow is added.

The catalog is seeded with 12 products only when empty. Existing products, users, orders and data volumes are retained. The Java RabbitMQ consumer runs inside the API service. Old Python containers can be removed with `docker compose up -d --remove-orphans`; do not delete data volumes to migrate.

| Page | Route |
| --- | --- |
| Landing | `/` |
| Collection | `/shop` |
| Customer login / registration | `/login`, `/register` |
| Admin login / registration | `/admin/login`, `/admin/register` |
| Checkout | `/checkout` |
| Customer dashboard | `/account` (orders, favourites, profile) |
| Customer order tracking | `/account/orders/:id` |
| Admin dashboard | `/admin/dashboard` (overview, fulfilment, customers, inbox, search sync) |
| Order search | `/admin/search` |
| Product catalog | `/admin/catalog` |

## Architecture

- PostgreSQL stores users, password hashes, orders and immutable item snapshots.
- MongoDB stores flexible product documents with unique SKUs.
- After a SQL transaction commits, Java publishes a persistent order-ID message to RabbitMQ.
- A Java consumer reads the current SQL order and indexes Elasticsearch using external version checks. Failed deliveries retry five times, then enter `buymore.java.order-index.failed`.
- Search reads Elasticsearch only. `POST /api/search/reindex` lets an authenticated admin reconcile orders after a broker/index failure. Queued dual-write has a commit/publish gap; it is not an outbox or a distributed transaction.
- Spring Security enforces roles and order ownership. Sessions use HttpOnly cookies and CSRF tokens; passwords use BCrypt. Client-supplied user IDs cannot select checkout ownership.

## Local addresses

| Service | Address |
| --- | --- |
| Frontend | http://localhost:15174 |
| API health | http://localhost:18002/api/health |
| PostgreSQL | localhost:15434 |
| MongoDB | localhost:27020 |
| Elasticsearch | http://localhost:19202 |
| RabbitMQ | localhost:15675 |
| RabbitMQ console | http://localhost:25675 |

All exposed services bind to localhost. Nginx supports HTTP without certificates and enables local HTTPS when `frontend/certs/buymore.com.pem` and its key exist. Set `COOKIE_SECURE=true` for HTTPS-only deployment. Infrastructure credentials are local-development defaults; this prototype does not include password recovery, email verification, login rate limiting, payments or shipping integrations.

## Host development

```bash
docker compose up -d postgres mongo elasticsearch rabbitmq
# Export desired values from .env.example; Maven does not automatically load .env.
mvn -f backend/pom.xml spring-boot:run -Dspring-boot.run.arguments=--server.port=18002
```

In a second terminal:

```bash
cd frontend
npm ci
npm run dev
```

Vite proxies `/api` to `127.0.0.1:18002`. Docker Nginx proxies to `api:8000`.

## Tests

```bash
mvn -f backend/pom.xml test
# Integration tests require the backing services and a dedicated test database:
docker compose exec -T postgres createdb -U buymore buymore_test
mvn -f backend/pom.xml -Pintegration verify
npm --prefix frontend run build
```

Integration tests use PostgreSQL/MongoDB `buymore_test` and Elasticsearch `buymore-integration-orders`, and create uniquely named fixture accounts/products. They also use a separate RabbitMQ queue (`buymore.java.test-order-index`). CI starts fresh backing services.

See [authentication API](docs/java-auth-integration.md) and [handoff](docs/AI-HANDOFF.md).

## Customer and admin dashboards

Customers can view their own paginated order history, track the saved Pending → Processing → Shipped status, save favourites across devices, add an earlier order's available products to their cart at current prices, and edit their display name. Tracking reflects staff updates; there is no carrier tracking or delivery estimate. Original order prices remain unchanged. Unavailable products are reported by Buy again instead of silently added.

The admin dashboard includes five panels: a sales overview with a seven-day order-value chart, an oldest-first fulfilment queue, a searchable customer directory, a contact inbox with resolve/reopen actions, and search-sync checks with per-order retry. Overview figures come from PostgreSQL, independently of search availability. Order value is not a payment or revenue-settlement report. Sync checks compare versions for the latest 50 updated orders and label that sample explicitly; the existing `/api/search/reindex` endpoint remains available for full reconciliation. Inbox actions do not send email.

`/api/account/**` requires a signed-in account and scopes personal data to that session. `/api/admin/**` requires the admin role. Schema initialization adds `favourites`, a contact-message resolution flag, and an order-history index without removing existing data; the equivalent SQL is in `database/migrations/003_dashboards.sql`.

Both dashboards use responsive floating surfaces, keyboard-accessible navigation, and reduced-motion support. A shared footer serves the public pages, storefront, and dashboards. Integration tests cover ownership, role restrictions, saved favourites, current-price reorder data, profile validation, metrics, messages, and sync recovery. To run them against the preview stack, set `TEST_POSTGRES_PORT=15435`, `TEST_MONGO_PORT=27021`, `RABBITMQ_PORT=15676`, and `ELASTICSEARCH_URL=http://127.0.0.1:19203`, with the matching local service credentials. Tests still use dedicated `buymore_test` databases and a separate index and queue.

## License

[MIT](LICENSE)
