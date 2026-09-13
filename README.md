# E-Voting System

A secure, blockchain-backed electronic voting platform built with Spring Boot. Voters authenticate via Aadhaar and mobile OTP, cast votes that are recorded as cryptographically signed and hash-chained blocks, and results are published live through an admin-controlled dashboard.

🔗 **Live demo:** https://evoting-system-production-2194.up.railway.app

> This is a student/demo project simulating an election portal. It is not affiliated with, endorsed by, or connected to any real Election Commission or government body.

## Features

- **Voter registration & authentication** — Aadhaar validation with OTP verification (SMS via Fast2SMS, email OTP fallback)
- **Blockchain-secured ballots** — each vote is stored as a mined, digitally signed block in a hash chain; any tampering is detected automatically via chain validation
- **Merkle tree verification** — vote integrity checks at the block level
- **Admin dashboard** — candidate approval workflow, voter management, chain integrity audit, and results publishing
- **Live results** — real-time result streaming via Server-Sent Events (SSE)
- **Vote tracking** — voters can verify their vote was recorded without revealing who they voted for
- **Session & CSRF security** — Spring Security with cookie-based CSRF protection, hashed passwords, and no default credential exposure

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring Security, Spring Data JPA |
| Database | MySQL (HikariCP connection pooling) |
| Frontend | Thymeleaf templates, vanilla JS, CSS |
| Auth | Aadhaar + OTP (Fast2SMS / email), BCrypt password hashing |
| Blockchain | Custom hash-chain implementation with digital signatures |
| Deployment | Railway (app + managed MySQL) |
| Build | Maven |

## Project structure

```
src/main/java/com/example/E_voting_System/
├── blockchain/       # Block, chain service, mining & validation logic
├── config/           # Security, CSRF, scheduling, dev data seeding
├── controller/       # REST & page controllers
├── devTools/         # Manual console utilities (test data generation, hashing)
├── dto/              # Request/response payloads
├── entity/           # JPA entities
├── exception/        # Custom exceptions & global handler
├── repository/       # Spring Data repositories
├── service/          # Business logic (voting, OTP, results, signatures)
├── stream/           # SSE live-update streaming
└── util/             # Validators
```

## Running locally

**Prerequisites:** Java 21, Maven, a running MySQL instance

1. Clone the repo:
   ```
   git clone https://github.com/chandru-74/E_voting-_system.git
   ```

2. Set the required environment variables (see `application.properties` for the full list — none of these are committed to the repo):
   ```
   DB_USERNAME=root
   DB_PASSWORD=your_db_password
   AADHAAR_PEPPER=your_secret_pepper
   ADMIN_USERNAME=admin
   ADMIN_PASSWORD_HASH=your_bcrypt_hash
   FAST2SMS_API_KEY=your_api_key
   FAST2SMS_ENABLED=true
   ```

3. Run the app:
   ```
   ./mvnw spring-boot:run
   ```

4. Visit `http://localhost:8082`

## Security notes

- All secrets are externalized via environment variables — nothing sensitive is hardcoded or committed
- Aadhaar numbers are hashed with a server-side pepper before storage
- Each vote block is both hash-verified and digitally signed, so tampering is detectable at two independent layers
- Admin routes and vote-casting endpoints require authentication; public routes (results, candidate list, instructions) are explicitly allow-listed

## Disclaimer

This project is built for educational purposes to demonstrate secure system design (blockchain integrity, OTP-based auth, tamper detection). It is not a production election system and should not be used to conduct real elections.
