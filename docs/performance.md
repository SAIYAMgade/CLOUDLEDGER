# CloudLedger Performance & Load Benchmark Specification

## 1. Performance Goals

- **Transaction Ingestion Throughput:** > 500 transactions/sec on baseline containerized hardware.
- **API Latency Targets:**
  - $p50 < 30\text{ms}$
  - $p95 < 150\text{ms}$
  - $p99 < 350\text{ms}$
- **Database Connection Pool:** HikariCP with 20 maximum connections, ordered locking preventing thread starvation.
- **Memory Footprint:** Java 17 with G1 Garbage Collector configured at 75% max RAM percentage.

## 2. Load Testing Execution with k6

A reproducible k6 load script is provided in `k6/load-test.js`:

```bash
# Execute k6 load test against local or staging environment
k6 run k6/load-test.js
```
