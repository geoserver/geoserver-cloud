# GeoServer Cloud Monitoring with Prometheus + Grafana

This guide explains how to monitor your GeoServer Cloud deployment using Prometheus and Grafana.

## Overview

GeoServer Cloud applications expose Spring Boot Actuator endpoints on port **8081**, including:
- `/actuator/health` - Health check endpoints
- `/actuator/metrics` - Micrometer metrics
- `/actuator/prometheus` - Prometheus-formatted metrics

The monitoring stack includes:
- **Prometheus** - Time-series database for metrics collection (port 9091)
- **Grafana** - Visualization and dashboards (port 3000)

## Purpose and Scope

This monitoring setup provides **basic observability for local development** and serves as a **starting point** for building production monitoring infrastructure.

**What this is:**
- Simple Prometheus/Grafana stack for development environments
- Basic dashboard showing essential metrics (JVM, HTTP, service health)
- Foundation and reference implementation for custom observability solutions
- Example of Eureka-based service discovery integration

**What this is NOT:**
- Production-ready monitoring (no alerting, persistence config, security hardening)
- Comprehensive dashboard suite (intentionally kept simple)
- Replacement for enterprise observability platforms

**For production deployments**, you should:
- Customize dashboards for your specific needs and SLAs
- Configure alerting rules and notification channels
- Implement persistent storage with appropriate retention policies
- Integrate with your existing observability stack (Datadog, New Relic, etc.)
- Add security (authentication, TLS, network policies)
- Consider distributed tracing (OpenTelemetry, Jaeger, Zipkin)

## Quick Start

### 1. Start GeoServer Cloud with Monitoring

Add the `monitoring.yml` file when starting your environment:

```bash
cd compose

# For pgconfig backend with monitoring:
./pgconfig -f monitoring.yml up -d

# For datadir backend with monitoring:
./datadir -f monitoring.yml up -d
```

This starts all GeoServer Cloud services plus Prometheus and Grafana.

### 2. Access the Monitoring Tools

- **Grafana Dashboard**: http://localhost:3000
  - Default credentials: `admin` / `admin` (you'll be prompted to change on first login)

- **Prometheus UI**: http://localhost:9091
  - View targets, explore metrics, test queries

### 3. View Pre-configured Dashboard

In Grafana:
1. Navigate to **Dashboards** → **GeoServer Cloud Overview**
2. This basic starter dashboard shows:
   - Service availability (up/down status)
   - JVM heap memory usage
   - CPU usage per service
   - HTTP request rate
   - HTTP request duration
   - JVM thread count

**Note**: This dashboard is intentionally simple and serves as a starting point. You should customize it for your specific monitoring needs, add alerting rules, and create additional dashboards as required.

## Dashboard Preview

![GeoServer Cloud Dashboard](dashboard-screenshot.png)

*The dashboard provides basic observability metrics for development and debugging. Customize it to suit your production monitoring requirements.*

## Available Metrics

All Spring Boot services expose these metric categories:

### JVM Metrics
- `jvm_memory_used_bytes` - Memory usage by area (heap, non-heap)
- `jvm_memory_max_bytes` - Maximum memory
- `jvm_threads_live_threads` - Thread count
- `jvm_gc_pause_seconds` - Garbage collection pauses
- `jvm_classes_loaded_classes` - Loaded class count

### System Metrics
- `system_cpu_usage` - System CPU usage (0-1)
- `process_cpu_usage` - Process CPU usage (0-1)
- `system_load_average_1m` - System load average

### HTTP Metrics
- `http_server_requests_seconds_count` - Request count
- `http_server_requests_seconds_sum` - Total request duration
- `http_server_requests_seconds_max` - Maximum request duration

Labeled by: `service`, `uri`, `method`, `status`, `outcome`

### Tomcat Metrics
- `tomcat_threads_current_threads` - Current thread count
- `tomcat_threads_busy_threads` - Busy threads
- `tomcat_sessions_active_current_sessions` - Active sessions

### Database Connection Pool (HikariCP)
- `hikaricp_connections_active` - Active connections
- `hikaricp_connections_idle` - Idle connections
- `hikaricp_connections_pending` - Pending connection requests
- `hikaricp_connections_max` - Maximum pool size

### GeoServer-specific Metrics
- `geoserver_metrics_*` - Custom GeoServer metrics (if enabled)

### RabbitMQ Metrics
The RabbitMQ management plugin exposes metrics at http://rabbitmq:15692/metrics

## Exploring Metrics

### Using Prometheus UI

1. Go to http://localhost:9091
2. Click **Status** → **Targets** to see all scraped services
3. Click **Graph** to explore metrics with PromQL

Example queries:
```promql
# Services that are down
up == 0

# Average request rate per service
rate(http_server_requests_seconds_count[5m])

# 95th percentile request duration
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# JVM heap usage percentage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```

### Creating Custom Dashboards

In Grafana:
1. Click **+** → **Create Dashboard**
2. Add panels with Prometheus queries
3. Use the Prometheus data source (pre-configured)

## Checking Actuator Endpoints Directly

You can also access actuator endpoints directly from the host:

```bash
# Check health of a specific service (services don't expose 8081 to host by default)
# Note: container name depends on your backend (pgconfig, datadir, etc)
docker exec -it gscloud_dev_pgconfig-wms-1 curl localhost:8081/actuator/health

# Or access via the gateway (if configured to proxy actuator endpoints)
curl http://localhost:9090/geoserver/cloud/wms/actuator/health
```

## Scaling Services

The monitoring setup uses **Eureka-based service discovery** to find all service replicas automatically! 🎉

### How It Works

All GeoServer Cloud services register themselves with the Eureka discovery service. Prometheus:
1. Queries the Eureka service registry every 30 seconds
2. Discovers **all instances** of each service, including all scaled replicas
3. Scrapes metrics from each instance independently
4. Labels each instance with service name, instance ID, and hostname

**This works perfectly with Docker Compose scaling** because Eureka tracks every individual container that registers.

### How to Scale

Scale any service to multiple replicas:

```bash
./pgconfig -f monitoring.yml up -d --scale wms=3 --scale wfs=2
```

Prometheus will automatically:
- Discover all 3 WMS replicas from Eureka
- Discover all 2 WFS replicas from Eureka
- Scrape metrics from each replica independently
- Label each with unique `instance_id`

### Viewing Scaled Services

In Prometheus:
1. Go to **Status** → **Targets**
2. You'll see **one target per replica** (e.g., 3 WMS targets if scaled to 3)
3. Each target has labels: `service`, `instance_id`, `hostname`, `application`
4. Go to **Status** → **Service Discovery** to see Eureka discovery in action

In Grafana:
- Metrics are collected from all replicas simultaneously
- Use `sum by (service)` to aggregate across all replicas of a service
- Use `instance_id` label to filter or view specific replicas
- Use `hostname` to identify individual containers

**Understanding the Service Availability Dashboard:**

The "Service Availability (Replica Count)" table shows:
- **Service**: The service name (wms, wfs, wcs, etc.)
- **Replicas UP**: Number of healthy replicas running
- **Replicas DOWN**: Number of unhealthy replicas (if any)

When you scale WMS to 3 replicas, you'll see:
```
Service | Replicas UP | Replicas DOWN
wms     | 3           | 0
```

This is the correct behavior! Each replica is an independent instance that:
- Runs in its own container
- Registers independently with Eureka
- Has its own health status
- Can fail independently

Prometheus tracks each replica separately, allowing you to:
- Monitor individual replica health
- Detect partial failures (e.g., 2/3 replicas healthy)
- View metrics per replica or aggregated

**Example Queries:**

Total request rate across all WMS replicas:
```promql
sum(rate(http_server_requests_seconds_count{service="wms"}[5m]))
```

Request rate per WMS replica:
```promql
rate(http_server_requests_seconds_count{service="wms"}[5m])
```

Memory usage of a specific replica:
```promql
jvm_memory_used_bytes{service="wms", instance_id="wms-service:172.18.0.5:8080", area="heap"}
```

Count of healthy replicas per service:
```promql
count by (service) (up{job="geoserver-cloud-services"} == 1)
```

## Configuration

### Prometheus Configuration

The setup includes **two Prometheus configurations**:

1. **`prometheus-eureka.yml`** (Default, Recommended)
   - Uses Eureka service discovery
   - Automatically discovers all replicas
   - Works perfectly with scaling
   - Requires Eureka discovery service to be running

2. **`prometheus.yml`** (Fallback)
   - Uses DNS-based service discovery (`tasks.<service>`)
   - Limited replica discovery in Docker Compose
   - Use if Eureka is disabled

**To switch configurations**, edit `compose/monitoring.yml` and change the volume mount:

```yaml
volumes:
  # For Eureka-based discovery (default):
  - ./prometheus-eureka.yml:/etc/prometheus/prometheus.yml:ro

  # OR for DNS-based discovery:
  - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
```

**To customize**, edit the active configuration file:
- Adjust scrape intervals (default: 15s)
- Add more static targets
- Configure alerting rules
- Modify relabeling rules

### Grafana Configuration

- Datasources: `compose/grafana/provisioning/datasources/`
- Dashboards: `compose/grafana/provisioning/dashboards/`

Add `.json` dashboard files to the dashboards directory - they'll be automatically loaded.

### Customizing Credentials

Set environment variables before starting:

```bash
export GRAFANA_USER=myuser
export GRAFANA_PASSWORD=mypassword
./pgconfig -f monitoring.yml up -d
```

## Monitoring Additional Components

### PostgreSQL

To monitor PostgreSQL, add the postgres_exporter:

1. Uncomment the postgres section in `compose/monitoring.yml`
2. Add this service:

```yaml
  postgres-exporter:
    image: prometheuscommunity/postgres-exporter:latest
    environment:
      DATA_SOURCE_NAME: "postgresql://geoserver:geoserver@geodatabase:5432/geoserver?sslmode=disable"
    ports:
      - "9187:9187"
    depends_on:
      - geodatabase
```

3. Uncomment the postgres job in `compose/prometheus.yml`

### RabbitMQ

RabbitMQ metrics are already configured if the management plugin is enabled (it is by default).

## Stopping the Monitoring Stack

```bash
# For pgconfig backend:
./pgconfig -f monitoring.yml down

# For datadir backend:
./datadir -f monitoring.yml down
```

To also remove volumes (WARNING: deletes all data including Prometheus metrics and Grafana dashboards):
```bash
./pgconfig -f monitoring.yml down -v
# or
./datadir -f monitoring.yml down -v
```

## Troubleshooting

### Not Seeing All Replicas in Prometheus (Eureka Discovery)

If you're using Eureka discovery but only see one instance per service:

1. **Check Eureka service registry:**
   - Go to http://localhost:8761 (Eureka console)
   - Verify all service instances are registered
   - Look for multiple instances of the scaled service

2. **Verify services are registering:**
   ```bash
   # Check if WMS instances are registered
   curl http://localhost:8761/eureka/apps/WMS-SERVICE | grep -o "<instance>.*</instance>"
   ```

3. **Check Prometheus service discovery:**
   - Go to http://localhost:9091/service-discovery
   - Look for `eureka_sd_configs` section
   - Verify discovered targets match Eureka registry

4. **Check Prometheus logs:**
   ```bash
   ./pgconfig -f monitoring.yml logs prometheus | grep -i eureka
   ```

### Using DNS Discovery Instead

If Eureka discovery isn't working or you prefer DNS:

1. Edit `compose/monitoring.yml`
2. Change the volume mount to use `prometheus.yml` instead of `prometheus-eureka.yml`
3. Restart: `./pgconfig -f monitoring.yml restart prometheus`

**Note**: DNS discovery has limitations - see the configuration section for details.

### Services showing as "Down" in Prometheus

1. Check if actuator endpoints are accessible:
   ```bash
   docker exec -it gscloud_dev_pgconfig-wms-1 curl localhost:8081/actuator/prometheus
   ```

2. Check Prometheus logs:
   ```bash
   ./pgconfig -f monitoring.yml logs prometheus
   ```

3. Verify services are on the same Docker network:
   ```bash
   docker network inspect gscloud_dev_pgconfig_default
   ```

### Grafana Dashboard Not Loading

1. Check Grafana logs:
   ```bash
   ./pgconfig -f monitoring.yml logs grafana
   ```

2. Verify Prometheus datasource:
   - Go to **Configuration** → **Data Sources**
   - Test the Prometheus connection

### High Memory Usage

The monitoring tools themselves consume resources. Adjust limits in `compose/monitoring.yml`:

```yaml
deploy:
  resources:
    limits:
      memory: 256M  # Reduce if needed
```

## Additional Resources

- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Pre-built Grafana Dashboards](https://grafana.com/grafana/dashboards/)
  - Dashboard ID 4701: JVM (Micrometer)
  - Dashboard ID 11378: Spring Boot Statistics
