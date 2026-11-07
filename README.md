# Tinky (PaymentSpeaker)

Payment support for small shops. A cashier creates an order with a VietQR code, the system picks up the bank transfer through SePay, matches it to the order, flags suspicious transfers, announces the payment by voice and prints the receipt.

Built with plain Servlet/JSP/JDBC (MVC), SQL Server and Tomcat. No framework, and no CDN or external font, so it runs offline.

## Features

- Point of sale with a three-level catalog, cart, discounts, VAT and three ways to pay: VietQR, cash, or cash plus QR
- SePay polling, duplicate and amount checks, fraud alerts and a manager queue for flagged transfers
- Spoken payment notice and receipt printing to a Windows printer
- Shifts, stock with history, audit log, shift and period reports
- Roles: admin, store manager, cashier, auditor, technician, affiliate
- Login by email one-time code or Google
- Optional modules that can be switched off: tax tools, online sales, affiliate program, AI assistant
- English and Vietnamese interface

## Requirements

- JDK 8, Tomcat 9, NetBeans
- SQL Server with TCP/IP enabled on port 1433
- Libraries are already in `web/WEB-INF/lib`

## Setup

1. In SQL Server Management Studio run `sql/01_schema.sql`, `sql/02_seed.sql` and `sql/03_checks.sql` in that order. In the last script `row_count` must equal `expected` in every row.
2. Edit `src/java/util/DBUtil.java` so the server, user and password match your machine.
3. In `src/java`, copy each `*.properties.example` file to the same name without `.example` and fill in the values:
   - `mail.properties`: SMTP account used to send login codes
   - `google-oauth.properties`: Google sign-in
   - `sepay.properties`: SePay token and bank account
   - `gemini.properties`: API key for the assistant (optional)
4. Run the project from NetBeans and open `/health` to check the database connection, then `/login`.

The seed creates one user per role (see `sql/02_seed.sql`) and demo orders, transfers and alerts. Passwords are not used. Without `mail.properties` no login code can be sent, and without `sepay.properties` only the demo transfers are available.

`sql/99_reset_demo.sql` drops everything. Run it only to start over, then run the three scripts again.

## Project layout

| Path | Content |
|---|---|
| `src/java/controller` | Servlets, grouped by area |
| `src/java/service` | Business logic |
| `src/java/dao` | All SQL, using `PreparedStatement` |
| `src/java/rules` | Fraud and order matching rules |
| `src/java/filter` | Authentication filter and role permissions |
| `web/WEB-INF/views` | JSP pages |
| `web/assets` | CSS and JavaScript |
| `sql` | Schema, seed data, checks and reset |
| `test-tools` | Stand-alone test programs |
| `tools` | Test data generator |

## Tests

The programs in `test-tools` are compiled with `javac` against `web/WEB-INF/lib`. Rule tests need no database; the others need the schema from step 1.
