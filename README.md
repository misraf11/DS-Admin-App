# DS Admin — DigitalSeba Admin App

Native Android (Kotlin + Jetpack Compose) app for the WooControl admin dashboard of **digitalseba.shop**.

* Live orders, products, customers, WhatsApp center, notifications, reports, sync logs, store health and settings — every feature of the web dashboard.
* Realtime: polls the dashboard every 5 s while open, auto-refreshes every screen when data changes and shows a banner + notification for new orders. Optional background alerts service.

## Server side
Upload `server/api/mobile.php` to the dashboard's `api/` folder (next to `live-state.php`). It creates an `app_tokens` table automatically on first login.

## Build
GitHub Actions builds a signed release APK on every push to `main` (see the *Actions* tab → artifact **DS-Admin-APK**).
