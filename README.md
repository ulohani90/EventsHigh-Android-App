# EventsHigh

**Local event discovery and in-app ticket booking for Indian cities—search, personalize, and buy without leaving the flow.**

## Overview

EventsHigh aggregated public event listings and routed booking through our ticketing stack. It targeted urban audiences in **Bangalore, Chennai, Delhi, Mumbai**, and other backend-supported cities while discovery was still fragmented across blogs and Facebook. I was the **sole Android developer**: I owned the client end-to-end—API integration, performance, releases, and crash triage—with backend and product on booking, growth, and rollouts.

## Key features

- **Event discovery & browsing** — Tabbed home (e.g. My Events, Explore, This Week, alerts), city switching, and map/grid views; **Volley** backed all list loads with shared request priority and cache control.
- **Ticket booking** — “Book” deep-linked into a **WebView** (`CustomUrlActivity`) against our **ticketing gateway**, so payment UX stayed flexible server-side while I hardened the shell (navigation, Chrome clients, upload callbacks).
- **Payment handoff** — Checkout ran on `ticketing.eventshigh.com`; I posted structured gateway payloads via **`GatewayUrlRequest`** with a **60s timeout and Volley retry policy** so flaky networks didn’t strand users mid-submit.
- **My Tickets** — Wallet synced via **`my_tickets_for_email`** JSON APIs into **`MyTicketObject`** models and RecyclerView cards (booking IDs, guest/session metadata).
- **Profiles & social** — Phone OTP and Facebook/Google flows for identity; friend/contact-driven discovery where permissions allowed.
- **Push notifications** — **Firebase Cloud Messaging** plus alarm-backed reminders and **geofence** transitions for location-triggered engagement—registration handled token refresh and routing into notification surfaces.
- **Growth & support** — **Branch.io**, HTTPS **App Links**, **Zendesk**, and Play Services–era analytics / install attribution.

## Tech stack

| Layer | Choices |
|--------|---------|
| **Language** | Java |
| **Architecture** | Layered **Activities / Fragments** with dedicated **`network`**, **`data`**, **`user`** packages (pre–AAC; no formal MVP/MVVM framework). |
| **Networking** | **Volley** (`JsonRequest`, `StringRequest`, shared **`VolleyHelper`** queue). |
| **Image loading** | **Glide** (Volley integration module in-tree). |
| **Local storage** | **SQLite** via **`SQLiteOpenHelper`** (`EventMarkDbHelper`, `StreamDbHelper`) for marks/stream caching—not Room. |
| **Push** | **Firebase Cloud Messaging** (`FirebaseMessaging` / legacy instance-ID wiring). |
| **Payments** | **Hosted checkout** in **WebView** + server-side PSP logic; client **POST** to ticketing gateway—provider-specific SDKs lived outside this repo. |
| **Other** | Facebook SDK, Branch, Zendesk, Google Maps / Places / Location / Analytics, Fabric **Crashlytics**, Support Library **27**, Custom Tabs, TimesSquare (calendar). |

## Technical highlights

The hardest work was **booking reliability on 2016-era Android**. Checkout ran in a **WebView**, so I owned failures others ignored: **file uploads** (`ValueCallback` / `onShowFileChooser`), SSL/cookies across redirects, and restoring a sane native stack after PSP redirects. I instrumented **`WebViewClient`**, guarded lifecycle teardown, and fed Crashlytics so we could replay real breakage.

**`GatewayUrlRequest`** treated ticketing POSTs as brittle RPC: **`DefaultRetryPolicy`** plus a **60s** gateway timeout absorbed flaky radios without silent failure—surfacing errors instead of ambiguous duplicates server-side. **Location**, **geofences**, and **FCM** had to coexist without killing batteries; **`EHInstanceIdService`** and **`UpdateAccountInfoService`** kept tokens and account sync coherent across logout and city changes.

**App Links** (`eventshigh.com`), **`eventshigh://`**, and **Branch** had to resolve to the correct **detail** or **grid** without stacking duplicate activities—mostly **URI routing and intent flags**, shipped under release pressure.

## Status

**App was live from 2016–2018. The company has since shut down. This codebase is preserved here for reference.**
