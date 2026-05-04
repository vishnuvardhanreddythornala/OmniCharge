---
marp: true
theme: default
paginate: true
header: 'OmniCharge Business Presentation'
footer: 'OmniCharge - Enterprise Mobile Recharge Platform'
---

# OmniCharge
**Enterprise Mobile Recharge Platform**
*Seamless, Scalable, and Secure Digital Payments*

---

# 1. The Problem We're Solving
- **Fragmented User Experience:** Users struggle with slow, multi-step recharge processes across different telecom operators.
- **System Downtime:** Legacy systems often crash during peak recharge windows (e.g., end of month).
- **Payment Failures:** Network issues lead to "zombie" transactions where money is deducted but recharges fail, causing customer frustration.
- **Lack of Observability:** Businesses lack real-time visibility into transaction health and system performance.

---

# 2. The OmniCharge Solution
OmniCharge is a unified, highly available mobile recharge platform that aggregates all telecom operators into a single, beautiful interface.

**Core Pillars:**
1. **Speed:** Sub-second plan retrieval using high-speed caching.
2. **Reliability:** Built on a distributed microservices architecture ensuring zero single points of failure.
3. **Security:** Enterprise-grade authentication and secure payment gateways.
4. **Transparency:** Automated SAGA orchestration ensures no transaction is ever left in an unknown state.

---

# 3. Key Business Features
- **Smart Operator Detection:** Instantly identifies telecom carriers (Airtel, Jio, Vi) simply from the phone number via Numverify API.
- **Frictionless Onboarding:** Login via Google OAuth or OTP (SMS/Email) — no passwords to remember.
- **Dynamic Plan Catalogs:** Real-time plan synchronization ensures customers always see the latest prices.
- **Instant Notifications:** Immediate SMS and Email receipts for every transaction.
- **Automated Expiry Reminders:** Drives retention by alerting users days before their plan expires.

---

# 4. Under The Hood: The Architecture
*OmniCharge is built for massive scale using modern cloud-native patterns.*

- **Microservices Ecosystem:** 9 decoupled services (User, Operator, Recharge, Payment, etc.) allowing independent scaling.
- **Event-Driven Design (RabbitMQ):** Services communicate asynchronously. If the notification service is busy, the recharge still succeeds instantly.
- **CQRS Pattern:** Reading plans is isolated from Admin updates. Customers experience zero lag even during heavy administrative catalog updates.
- **Containerized:** Fully Dockerized and deployed on an Azure Virtual Machine for high performance.

---

# 5. Resilience & Financial Integrity
*What happens when things go wrong? OmniCharge auto-corrects.*

- **SAGA Orchestration:** Complex transactions across microservices are strictly managed. If a recharge fails, the payment is automatically flagged for refund.
- **Zombie Sweeper:** Automated background tasks scan for abandoned Razorpay checkouts every 5 minutes and safely close them out.
- **Circuit Breakers (Resilience4j):** If a telecom operator's API goes down, OmniCharge automatically triggers fallbacks to prevent cascading failures.
- **Zero Log Loss:** An "Outbox Pattern" guarantees every audit trail is saved to disk even if the message broker crashes.

---

# 6. Enterprise-Grade Security
- **API Gateway:** A reactive, centralized gateway intercepts all traffic, ensuring no internal service is exposed to the public internet.
- **Stateless JWT Auth:** Cryptographically signed tokens with a strict 1-hour expiry and real-time Redis blacklisting.
- **Admin 2FA:** Administrative portals are locked behind strict Two-Factor Authentication.
- **Rate Limiting:** Built-in protection against DDoS and brute-force attacks via Redis.

---

# 7. Real-Time Business Observability
*We don't guess if the system is healthy; we know.*

- **Grafana & Prometheus:** Live dashboards showing API latency, system load, and business metrics.
- **Loki Log Aggregation:** Millions of log lines across 9 services are searchable from a single pane of glass.
- **Distributed Tracing (Zipkin):** Every transaction is tagged with a unique ID, allowing us to trace a customer's journey from button click to database write.

---

# 8. Future Roadmap & Scaling
- **Phase 1 (Current):** Stable MVP on Azure VM with full core functionality.
- **Phase 2 (Next Quarter):** Introduce automated B2B bulk recharges and operator analytics.
- **Phase 3:** Migrate to Kubernetes (AKS) for infinite auto-scaling during festive peaks.

---

# Thank You!
**OmniCharge Team**
*Ready for Q&A and Live Demo*

---

# 🎙️ SPEAKER NOTES & DEMO GUIDE
*(Keep these handy during your presentation)*

**Demo Flow Suggestion:**
1. **Show the UI:** Open `https://vishnuvardhanreddythornala.github.io/OmniCharge/` and demonstrate how fast operator detection works.
2. **Login:** Show the smooth "Continue with Google" flow.
3. **Recharge:** Do a live 10 Rs test recharge through Razorpay.
4. **Behind the scenes:** Open Grafana (`http://20.219.10.246:3000`) and show the spike in metrics from your recharge!
5. **Swagger:** Open the API Gateway Swagger UI to show how professionally the APIs are documented.
