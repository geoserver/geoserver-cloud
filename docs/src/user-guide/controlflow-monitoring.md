# Monitoring control-flow throttling

GeoServer Cloud's control-flow extension throttles OWS requests instead of rejecting them: once a rule's concurrency limit is reached, further requests wait in a priority queue until a slot frees up, or until a configurable timeout expires and they are rejected. Because throttled requests wait rather than fail outright, the signal that a rule is limiting traffic is requests piling up and blocking, not errors in a log. This tutorial starts the development compose stack with Prometheus and Grafana, fires enough concurrent WMS requests to trip the default limits, and follows the throttling live on the "GeoServer Control-Flow" Grafana dashboard: rule saturation climbing to 100%, blocked requests appearing, and the effect of tightening a rule, overriding it with an environment variable, adding a per-IP limit, or turning on priority queuing.

## Prerequisites

- Docker Engine with Compose v2 (`docker compose version` reports a v2.x client).
- A local clone of the geoserver-cloud repository.
- About 4 GB of free RAM: the stack runs GeoServer Cloud's services alongside Consul, RabbitMQ, a database, Prometheus, and Grafana.

## 1. Start the stack with monitoring

Start the datadir-backed development stack with the monitoring overlay:

```bash
cd compose
./datadir -f monitoring.yml up -d
```

The `datadir` script wires in the bundled sample data directory, the classic GeoServer demo workspaces (`topp`, `ne`, `sf`, and others); `-f monitoring.yml` adds Prometheus and Grafana to the same stack. Image pulls and sample catalog extraction take a minute or two on the first run.

Check progress with:

```bash
./datadir -f monitoring.yml ps
```

Once every service reports healthy, open:

- Grafana at [http://localhost:3000](http://localhost:3000), credentials `admin` / `admin`.
- The GeoServer web UI through the gateway, at [http://localhost:9090/geoserver/cloud/web/](http://localhost:9090/geoserver/cloud/web/). Port 9090 is this stack's default gateway port, set by `GATEWAY_PORT` in `compose/.env`.
- Prometheus at [http://localhost:9091](http://localhost:9091), optional for this walkthrough, useful later for running PromQL queries directly.

## 2. Find the baseline

In Grafana, open **Dashboards -> GeoServer Control-Flow**. Its **Configured Rules** table lists the rules active in `config/geoserver_control_flow.yml`, and each service loads only the rules that apply to it: the file's first document holds the universal settings (the queue timeout and commented per-user/per-IP examples; there is no global limit by default), and per-service profile documents add the specific rules. The `wms` service reports `ows.wms` and `ows.wms.getmap`; `wps` reports `ows.wps.execute`; `gwc` reports `ows.gwc` plus an `ows.wms.getmap` rule that throttles seed-triggered rendering; `wfs` and `wcs` load no rules by default; `restconfig` and `webui` serve no OWS requests and publish no control-flow metrics at all. The file's `timeout` setting applies to queued requests instead of appearing as a rule. With no traffic yet, every rule's running count is 0.

The dashboard's `$application` and `$rule` variables narrow the view to one service or rule; leave them on "All" for now.

For the same numbers without Grafana, query the wms pod's actuator endpoint directly:

```bash
./datadir -f monitoring.yml exec wms curl -s localhost:8081/actuator/prometheus | grep geoserver_controlflow
```

Each rule shows up as a `geoserver_controlflow_rule_*` gauge family labeled by `rule`; with the stack idle, every `_running` value is 0.

## 3. Generate load

`topp:states`, the classic census and state-boundaries demo layer bundled in the sample data directory, is a convenient target: small enough to render quickly, and present in every stock GeoServer Cloud sample catalog. The requests authenticate as the admin user: this stack runs GeoServer ACL with no authorization rules, and anonymous requests see no layers at all (GeoServer hides unauthorized layers instead of rejecting the request).

Fire enough concurrent `GetMap` requests to exceed the default `ows.wms.getmap` limit, four times the container's allocated CPU count:

```bash
export GEOSERVER_URL="http://localhost:9090/geoserver/cloud"
export GETMAP="$GEOSERVER_URL/wms?service=WMS&version=1.1.1&request=GetMap&layers=topp:states&bbox=-125,24,-66,50&width=800&height=400&srs=EPSG:4326&format=image/png"

for round in $(seq 1 8); do
  for i in $(seq 1 60); do
    curl -s -o /dev/null -u admin:geoserver "$GETMAP" &
  done
  wait
done
```

The outer loop sustains the load for several seconds, long enough for at least one Prometheus scrape (15 second interval) to land mid-run. Switch to Grafana while it executes. On the **GeoServer Control-Flow** dashboard, **Rule Saturation (running / limit)** climbs toward 100% for `ows.wms.getmap`, **Blocking Episodes** shows the rule going active, and on the **GeoServer Cloud Overview** dashboard **Control-Flow Blocked Requests** goes non-zero. That is the throttle working as intended: requests are queuing behind the limit, not failing.

## 4. Tighten a rule and watch it propagate

Edit `config/geoserver_control_flow.yml` and, in the wms service's profile document (`wms_service`), replace the `ows.wms.getmap` limit with a fixed, much lower value:

```yaml
'[ows.wms.getmap]': "2"
```

Restart the wms service to pick up the change:

```bash
./datadir -f monitoring.yml restart wms
```

Run the load loop from [Generate load](#3-generate-load) again. The **Configured Rules** table now shows `ows.wms.getmap` at limit 2, and **Rule Saturation** pins at 100% for the whole run: two requests running, the rest queued, since the loop's concurrency is far above the new limit.

## 5. Override a rule with an environment variable

Editing a tracked configuration file works for development, but a deployment usually wants to tune a limit per container without touching the shared file. Every rule can be set or overridden through an environment variable. The naming follows Spring Boot's relaxed binding, which is not obvious for map keys: uppercase the property path, turn dots into underscores, drop the dash in `control-flow`, and everything after `PROPERTIES_` is the rule key. `'[ows.wms.getmap]'` becomes `GEOSERVER_EXTENSION_CONTROLFLOW_PROPERTIES_OWS_WMS_GETMAP`.

Create a compose overlay named `controlflow-override.yml` in the `compose` directory:

```yaml
services:
  wms:
    environment:
      GEOSERVER_EXTENSION_CONTROLFLOW_PROPERTIES_OWS_WMS_GETMAP: "6"
```

and recreate the wms service with it:

```bash
./datadir -f monitoring.yml -f controlflow-override.yml up -d wms
```

Once wms reports healthy again, **Configured Rules** shows `ows.wms.getmap` at limit 6: the environment variable wins over the value in `config/geoserver_control_flow.yml`, including the 2 set in the previous step. Environment variables override or add rules per key; they cannot remove a rule defined in the file. Recreating the service without the overlay (`./datadir -f monitoring.yml up -d wms`) restores the file-defined limits. The header comments of `config/geoserver_control_flow.yml` document more override forms, including `SPRING_APPLICATION_JSON` for rule keys that cannot be spelled as a variable name, such as output formats containing a slash.

## 6. Watch per-IP queues

The commented-out `[ip]` rule in the same file limits concurrent requests per client address, independently of which OWS operation they call. Uncomment it:

```yaml
'[ip]': "6"
```

Restart wms again and re-run the load loop:

```bash
./datadir -f monitoring.yml restart wms
```

A new `ip` rule appears in **Configured Rules**, and **Active Per-User / Per-IP Queues** shows one active queue, since every request in the loop comes from the same client address, climbing to the configured limit of 6 while the rest wait behind it.

## 7. See the priority queue

The **Waiting in Priority Queue** panel stays empty in all the runs above, and that is expected: without a priority provider, rules use a simple blocker whose queued requests are not individually observable. Configuring `ows.priority.http` switches every global and per-OWS rule to a priority blocker that reports how many requests are parked in its queue, and lets clients influence their place in it through an HTTP header.

In the first document of `config/geoserver_control_flow.yml`, under `properties`, add:

```yaml
'[ows.priority.http]': "gs-priority,3"
```

The pair names the header to read (`gs-priority`, any name works) and the default priority (3) for requests without it. Higher numbers win; equal priorities drain first-in first-out. Restart wms and re-run the load loop from [Generate load](#3-generate-load): **Waiting in Priority Queue** now shows the queued requests for `ows.wms` and `ows.wms.getmap` while the run lasts. The per-IP rule from the previous step keeps its own queue accounting and is not affected by the priority provider.

To see priorities reorder the queue, start a batch of default-priority requests in the background, then immediately run a second batch that jumps ahead of it:

```bash
for i in $(seq 1 30); do curl -s -o /dev/null -u admin:geoserver "$GETMAP" & done

time ( for i in $(seq 1 30); do curl -s -o /dev/null -u admin:geoserver -H "gs-priority: 10" "$GETMAP" & done; wait )
wait
```

Although the high-priority batch enters a queue already full of default-priority requests, the blocker releases its requests first: the timed batch completes well before the background one drains.

## 8. Clean up

Stop the stack:

```bash
./datadir -f monitoring.yml down
```

`config/geoserver_control_flow.yml` is a tracked file in the repository, not a container volume: the edits from this tutorial are still on disk. Revert them, and delete the compose overlay if you created it:

```bash
git checkout -- config/geoserver_control_flow.yml
rm -f controlflow-override.yml
```

!!! note
    `down` leaves the Prometheus and Grafana volumes in place, preserving your dashboards and metric history across restarts. Add `-v` to drop them too.

## Where to go next

- The [control-flow metrics reference](controlflow-metrics-reference.md) for the full metric inventory, example PromQL queries, and the Grafana recipes behind the dashboard.
- [MONITORING.md](https://github.com/geoserver/geoserver-cloud/blob/main/compose/monitoring/MONITORING.md) for the dev compose monitoring stack itself: ports, credentials, Consul-based scraping, and its limitations.
- The [externalized configuration guide](../configuration/index.md) for how GeoServer Cloud services pick up configuration changes.
- The upstream [GeoServer Control Flow extension documentation](https://docs.geoserver.org/main/en/user/extensions/controlflow/index.html) for every rule syntax control-flow supports, including per-user rate limiting and IP blacklists.
