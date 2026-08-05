# Control-flow metrics reference

Services running the control-flow extension export throttling gauges on the actuator
Prometheus endpoint (`:8081/actuator/prometheus` in the standard deployments) whenever a
Micrometer registry is available and `geoserver.metrics.enabled` is not `false`. The
[Monitoring control-flow](controlflow-monitoring.md) tutorial shows them in action on the
dev compose stack; this page is the lookup reference for the metrics and the queries built
on them.

Metric names below use the Prometheus form. The actuator's `/actuator/metrics` endpoint
lists the same meters in dotted form (`geoserver.controlflow.requests.running`).

## Global gauges

| Metric | Meaning |
|--------|---------|
| `geoserver_controlflow_requests_running` | requests currently admitted past control-flow |
| `geoserver_controlflow_requests_blocked` | requests currently blocked inside control-flow acquisition |

## Per-rule gauges

Labeled `rule` (the configuration key, e.g. `ows.wms.getmap`, `user`, `ip`) and
`controller` (the implementing class). Label cardinality is bounded by the configured
rules; metrics are never labeled by request user or client address, and a single-IP rule
embeds its configured address in the rule key:

| Metric | Meaning |
|--------|---------|
| `geoserver_controlflow_rule_limit` | configured concurrency or queue limit |
| `geoserver_controlflow_rule_running` | requests currently admitted under the rule |
| `geoserver_controlflow_rule_waiting` | requests parked in the rule's priority queue |
| `geoserver_controlflow_rule_queues_active` | live per-user or per-IP queue count |
| `geoserver_controlflow_rule_rate_limit` | configured maximum requests per rate window |

## Example PromQL queries

Current state:

```promql
# which rule is throttling: saturation of each rule against its limit, in percent
100 * geoserver_controlflow_rule_running / geoserver_controlflow_rule_limit

# is anything blocked right now, per service
sum by (application) (geoserver_controlflow_requests_blocked)

# free slots per rule (how much headroom is left)
geoserver_controlflow_rule_limit - geoserver_controlflow_rule_running

# rules running exactly at their limit right now
geoserver_controlflow_rule_running >= geoserver_controlflow_rule_limit

# requests parked in priority queues (needs ows.priority.http configured)
geoserver_controlflow_rule_waiting > 0

# busiest per-user/per-IP rules by live queue count
topk(5, geoserver_controlflow_rule_queues_active)
```

Over time and across replicas:

```promql
# services that throttled at any point in the last 15 minutes
max_over_time(geoserver_controlflow_requests_blocked[15m]) > 0

# share of the last hour each pod spent throttling (0 to 1, sampled per scrape)
avg_over_time((geoserver_controlflow_requests_blocked > bool 0)[1h:15s])

# load distribution across the replicas of each service
sum by (application, instance) (geoserver_controlflow_requests_running)

# aggregate configured capacity per rule across all replicas of a service
sum by (application, rule) (geoserver_controlflow_rule_limit)
```

Alert-shaped queries, ready to lift into an alerting system:

```promql
# sustained throttling: pair with "for: 5m" in an alert rule
sum by (application) (geoserver_controlflow_requests_blocked) > 0

# a rule pinned at its limit: pair with "for: 10m"
geoserver_controlflow_rule_running >= geoserver_controlflow_rule_limit
```

The `application` label in these examples comes from the dev stack's Prometheus relabeling
(the Consul service name); adjust it to whatever service-identity label your scrape
configuration applies.

## Grafana building blocks

The **GeoServer Control-Flow** dashboard shipped with the dev compose stack
([geoserver-controlflow.json](https://github.com/geoserver/geoserver-cloud/blob/main/compose/monitoring/grafana/provisioning/dashboards/geoserver-controlflow.json))
is the worked example for all of these; the recipes below are its load-bearing pieces for
reuse in your own dashboards.

Template variables, chained so the rule list follows the service selection:

```promql
label_values(geoserver_controlflow_requests_running, application)
label_values(geoserver_controlflow_rule_limit{application=~"$application"}, rule)
```

Rule saturation timeseries: plot the percent ratio with unit `percent`, `max` 100, and
threshold steps at 70 and 90:

```promql
100 * geoserver_controlflow_rule_running{application=~"$application", rule=~"$rule"}
    / geoserver_controlflow_rule_limit{application=~"$application", rule=~"$rule"}
```

Blocking episodes as a state timeline: map 0 to OK and 1 to Blocked with value mappings:

```promql
max by (application) (geoserver_controlflow_requests_blocked{application=~"$application"} > bool 0)
```

Live rule inventory as a table: four instant queries in `table` format (`rule_limit`,
`rule_running`, `rule_waiting`, `rule_queues_active`), combined with the **Merge**
transformation and an **Organize fields** step that renames the value columns and hides
the scrape labels.

## Behavior notes

- Rule changes appear on the scrape after the control-flow configuration reloads.
- Each service loads only the rules that apply to it; see the per-service profile
  documents in `config/geoserver_control_flow.yml` and the
  [externalized configuration guide](../configuration/index.md).
- `geoserver_controlflow_rule_waiting` reports only on rules backed by a priority
  blocker, configured through `ows.priority.http`.
- With the data-directory configuration (`use-properties-file=true`) all per-rule gauges
  work except `rule_running` on rules backed by a priority blocker (upstream keeps that
  count private).
