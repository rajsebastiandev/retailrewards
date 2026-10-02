# Rewards API

A Spring Boot REST API for a retailer's rewards program. It works out the reward points each customer earns per month and in total over a three-month period of transactions. New purchases can be recorded through the API, and they show up in the rewards straight away.

## Rewards rule

For each transaction:

- **2 points** for every dollar spent **over $100**, plus
- **1 point** for every dollar spent **between $50 and $100**.

Example: a $120 purchase = 2 × $20 + 1 × $50 = **90 points**.

Points are awarded for **whole dollars only**. Cents are dropped before calculating, so $120.75 earns 90 points and $50.99 earns 0.

## Tech stack

- Java 21, Spring Boot 3.5 (Spring Web, Bean Validation)
- In-memory collections for storage (no database)
- JUnit 5, AssertJ and MockMvc for tests
- Maven, with the wrapper included (no local Maven install needed)

## Running

```bash
./mvnw spring-boot:run          # macOS / Linux
mvnw.cmd spring-boot:run        # Windows
```

The API starts on `http://localhost:8080`.

Run the tests:

```bash
./mvnw test
```

## API

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/rewards` | Monthly and total points for every customer |
| GET | `/api/rewards/{customerId}` | Monthly and total points for one customer |
| POST | `/api/transactions` | Record a new purchase |
| GET | `/api/transactions` | List purchases, oldest first (optional `?customerId=`) |
| GET | `/api/transactions/{id}` | Get one purchase |

The rewards endpoints accept the optional query parameters `from` and `to`. These are ISO dates (`yyyy-MM-dd`) and both are inclusive. If you leave them out, the period runs from the earliest to the latest recorded transaction.

Every month in the period appears in `monthlyRewards`, including months where the customer earned 0 points.

### Examples

`GET /api/rewards/1`

```json
{
  "customerId": 1,
  "customerName": "Alice Johnson",
  "monthlyRewards": [
    { "month": "2026-07", "points": 115 },
    { "month": "2026-08", "points": 250 },
    { "month": "2026-09", "points": 160 }
  ],
  "totalPoints": 525
}
```

`GET /api/rewards?from=2026-08-01&to=2026-09-30` returns the same customer structure for every customer, wrapped like this:

```json
{
  "from": "2026-08-01",
  "to": "2026-09-30",
  "customers": [ { "customerId": 1, "customerName": "Alice Johnson", "monthlyRewards": [ ... ], "totalPoints": 410 }, ... ]
}
```

### Recording a purchase

`POST /api/transactions`

```json
{ "customerId": 5, "amount": 120.00, "date": "2026-09-20" }
```

This returns `201 Created`, with a `Location` header pointing to the new transaction. The id is generated and continues the sample data's sequence: the sample data uses `T1001`–`T1019`, so the first new purchase is `T1020`, then `T1021`, and so on. The response also includes the points the purchase earned:

```json
{ "id": "T1020", "customerId": 5, "customerName": "Eva Martinez", "amount": 120.00, "date": "2026-09-20", "points": 90 }
```

Rewards are calculated on every request, so `GET /api/rewards/5` now shows 90 points for September.

Validation rules: `customerId` must belong to an existing customer, `amount` must be at least 0.01 with no more than 2 decimal places, and `date` (`yyyy-MM-dd`) must not be in the future. If a purchase falls outside the current Jul-Sep range, the default rewards period stretches to include it.

Recorded purchases are kept **in memory**, so restarting the app resets everything to the sample data.

### Errors

Errors come back as [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) problem details:

| Case | Status |
|---|---|
| Unknown customer id or transaction id | 404 |
| `from` after `to` | 400 |
| Malformed date or non-numeric customer id | 400 |
| Invalid transaction fields (the response lists each field under `errors`) | 400 |
| Request body that isn't valid JSON, or a badly formatted date | 400 |

```json
{ "type": "about:blank", "title": "Customer not found", "status": 404, "detail": "Customer 999 not found", "instance": "/api/rewards/999" }
```

### Postman

A ready-made collection is in [`postman/rewards.postman_collection.json`](postman/rewards.postman_collection.json):

1. Start the app (`mvnw spring-boot:run`).
2. In Postman, choose **Import** and select the file.
3. Send the requests one at a time, or use **Run collection** to run them all.

The collection has three folders:

- **Rewards** and **Errors** check the expected points for the sample data.
- **Transactions** lists purchases, then runs a four-step flow: read Eva's rewards, add a $120 purchase, fetch it, and check that her rewards went up by 90. It also covers the validation and 404 cases for the new endpoints.

Run the collection against a freshly started app. The fixed totals in the Rewards folder assume only the sample data, and the Transactions folder runs last. The Transactions flow compares before and after, so you can run it again at any time. The `baseUrl` collection variable defaults to `http://localhost:8080`.

To run it from the command line with [Newman](https://www.npmjs.com/package/newman):

```bash
npx newman run postman/rewards.postman_collection.json
```

## Sample data set

The data set is in [`src/main/resources/data/transactions.json`](src/main/resources/data/transactions.json) and covers July to September 2026. It was chosen to exercise the edge cases of the rule:

| Customer | What it demonstrates | Jul | Aug | Sep | Total |
|---|---|---:|---:|---:|---:|
| 1 Alice Johnson | The spec's $120 → 90 example, fractional amounts ($75.50, $130.25), exactly $100 | 115 | 250 | 160 | **525** |
| 2 Bob Smith | Boundaries: exactly $50 → 0, $51 → 1, $99.99 → 49, $101 → 52 | 1 | 49 | 602 | **652** |
| 3 Carol Davis | A large purchase ($500 → 850), a month with only a sub-$50 purchase, month-end and month-start dates | 850 | 0 | 15 | **865** |
| 4 David Lee | No purchases in some months (those months show 0) | 0 | 220 | 0 | **220** |
| 5 Eva Martinez | Only purchases under $50 (included with 0 points) | 0 | 0 | 0 | **0** |

The data loads once at startup into in-memory collections and is validated: it rejects negative amounts, unknown customers and duplicate ids. To use a different file, set `rewards.data-file`, for example `--rewards.data-file=file:/path/to/data.json`.

## Project layout

```
src/main/java/com/retailer/rewards
├── controller/   RewardsController         rewards endpoints
│                 TransactionController     record / list purchases
├── service/      RewardPointsCalculator    points for one transaction
│                 RewardsService            monthly / total aggregation, date range handling
│                 TransactionService        recording and lookup of purchases
├── repository/   RewardsDataRepository     data access interface
│                 InMemoryRewardsDataRepository   thread-safe in-memory store, seeded from JSON
├── model/        Customer, Transaction
├── dto/          RewardsSummary, CustomerRewards, MonthlyRewards,
│                 CreateTransactionRequest, TransactionResponse
└── exception/    GlobalExceptionHandler and domain exceptions
```

The repository sits behind an interface, so you could swap in a JPA or database implementation without changing the service or controller.

## Tests

| Test | Covers |
|---|---|
| `RewardPointsCalculatorTest` | Rule boundaries ($50, $100), cents, large and invalid amounts |
| `RewardsServiceTest` | Monthly grouping, zero months, date filtering, unknown customer, invalid range, empty data |
| `InMemoryRewardsDataRepositoryTest` | Seed loading, data validation, saving with generated ids, concurrent saves |
| `RewardsControllerIntegrationTest` | Full HTTP stack against the seed data, including error responses |
| `TransactionControllerIntegrationTest` | Recording purchases and seeing the rewards change, lookups, validation and 404 errors |
