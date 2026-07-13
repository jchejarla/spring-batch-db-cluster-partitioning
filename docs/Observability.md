# Observability

Everything the cluster knows about itself lives in the coordination database, so it can be inspected
with ordinary SQL — but you rarely need to. The library ships **Spring Boot Actuator** integrations
that surface the same state as JSON, in two flavours:

- **Node-centric** — "what does the cluster look like right now, node by node?"
- **Job-centric** — "for this job execution, how are its partitions placed and progressing?"

Plus health indicators and an `/actuator/info` contributor. All of it is read-only and runs off the
coordination tables, off the hot polling path.

!!! warning "You must expose these endpoints"
    Like all non-`health` actuator endpoints, these are **not exposed over HTTP by default**. Add the
    ones you want to `management.endpoints.web.exposure.include`:

    ```yaml
    management:
      endpoints:
        web:
          exposure:
            include: "health,info,batch-cluster,batch-cluster-jobs"
      endpoint:
        health:
          show-details: always   # needed to see the cluster health details below
    ```

    The endpoint IDs are `batch-cluster` and `batch-cluster-jobs`. They only register when clustering is
    enabled (`spring.batch.cluster.enabled=true`).

---

## Health — `/actuator/health`

The library contributes **two** health components. With `show-details: always`:

```json
{
  "components": {
    "batchCluster": {
      "status": "UP",
      "details": {
        "Total Nodes in Cluster": "2",
        "Total Active Nodes": "2"
      }
    },
    "batchClusterNode": {
      "status": "UP",
      "details": {
        "Node Id": "node-8bde6cfa-0505-4f0a-a542-2c37a16b1936",
        "Node Status": "ACTIVE",
        "Current Load": "0",
        "Start Time": "Fri Jul 10 11:46:19 EDT 2026",
        "Last Heartbeat Update Time": "Fri Jul 10 11:46:40 EDT 2026"
      }
    }
  }
}
```

| Component | Reports | `UP` when | `DOWN` when |
|---|---|---|---|
| `batchCluster` | cluster-wide view: total registered nodes and how many are `ACTIVE` | at least one node is registered | the registry is empty (e.g. the database is unreachable) |
| `batchClusterNode` | **this** node's own id, status, load, start time, last heartbeat | this node's status is `ACTIVE` | this node is `UNREACHABLE` (its own heartbeat has lapsed) |

`batchClusterNode` going `DOWN` is a useful readiness signal — a node that has lost its heartbeat
self-fences (stops claiming work), and this reflects that before the master removes it.

---

## Info — `/actuator/info`

Publishes the running library version and the **effective** cluster settings — handy for confirming
what timings a node is actually using (support, debugging, drift checks):

```json
{
  "Batch Clustering Properties": {
    "Batch clustering version": "3.0.0",
    "Node heartbeat interval (milli seconds)": 1000,
    "Check for tasks polling interval (milli seconds)": 1000,
    "Node unreachable marking threshold (milli seconds)": 3000,
    "Node delete threshold (milli seconds)": 5000,
    "Master task status check interval (milli seconds)": 500,
    "Check for orphaned tasks interval (milli seconds)": 1000,
    "Concurrency limit per node": 10,
    "Node unreachable marking-thread interval (milli seconds)": 5000,
    "Node cleanup-thread interval (milli seconds)": 5000
  }
}
```

The version is read from the jar manifest, so it always matches the artifact on the classpath.

---

## Cluster overview (node-centric)

### `/actuator/batch-cluster`

Every node currently in the registry, with its status, host, heartbeat, and live load:

```json
{
  "totalNodes": 2,
  "nodes": [
    {
      "Node Id": "node-8bde6cfa-0505-4f0a-a542-2c37a16b1936",
      "Host": "Mac.lan",
      "Status": "ACTIVE",
      "Started At": "2026-07-10T15:46:19.954Z",
      "Last Heartbeat": "2026-07-10T15:46:29.963Z",
      "Current Load (# of tasks)": 0
    },
    {
      "Node Id": "node-d4bfd102-f884-4fdc-9b7c-eefa7ff155b1",
      "Host": "Mac.lan",
      "Status": "ACTIVE",
      "Started At": "2026-07-10T15:46:22.625Z",
      "Last Heartbeat": "2026-07-10T15:46:30.639Z",
      "Current Load (# of tasks)": 0
    }
  ]
}
```

`Status` is `ACTIVE` or `UNREACHABLE`. `Current Load (# of tasks)` is the number of partitions the node
is running right now — the same figure the `LEAST_LOADED` assignment strategy uses.

### `/actuator/batch-cluster/{nodeId}`

One node's detail (the raw `ClusterNodeInfo`):

```json
{
  "nodeId": "node-8bde6cfa-0505-4f0a-a542-2c37a16b1936",
  "hostIdentifier": "Mac.lan",
  "nodeStatus": "ACTIVE",
  "currentLoad": 0,
  "startTime": "2026-07-10T15:46:19.954Z",
  "lastHeartbeatTime": "2026-07-10T15:46:30.963Z"
}
```

---

## Job view (job-centric)

This is the view for answering "where did the partitions of *this* job go, and how are they doing?"

### `/actuator/batch-cluster-jobs`

The jobs currently coordinated by the cluster, one row each:

```json
[
  {
    "jobExecutionId": 1,
    "masterNode": "node-d3706bd7-0d54-4011-aa53-49ec6395ab53",
    "masterStepName": "multiNodeWorkerStep",
    "coordinationStatus": "STARTED"
  }
]
```

`coordinationStatus` is `CREATED` → `STARTED` → `COMPLETED` (or `ABANDONED` if a master was lost and the
job was recovered).

### `/actuator/batch-cluster-jobs/{jobExecutionId}`

The full breakdown: the master, a status histogram, and every partition with the node it landed on.

**While running** — four partitions claimed across two nodes:

```json
{
  "jobExecutionId": 1,
  "masterNode": "node-d3706bd7-0d54-4011-aa53-49ec6395ab53",
  "coordinationStatus": "STARTED",
  "partitionCount": 4,
  "statusCounts": { "CLAIMED": 4 },
  "partitions": [
    { "stepExecutionId": 2, "partitionKey": "multiNodeWorkerStep:0", "assignedNode": "node-d3706bd7-0d54-4011-aa53-49ec6395ab53", "status": "CLAIMED" },
    { "stepExecutionId": 3, "partitionKey": "multiNodeWorkerStep:1", "assignedNode": "node-c96bd358-e2f6-425e-9f04-37c4d210efc9", "status": "CLAIMED" },
    { "stepExecutionId": 4, "partitionKey": "multiNodeWorkerStep:2", "assignedNode": "node-d3706bd7-0d54-4011-aa53-49ec6395ab53", "status": "CLAIMED" },
    { "stepExecutionId": 5, "partitionKey": "multiNodeWorkerStep:3", "assignedNode": "node-c96bd358-e2f6-425e-9f04-37c4d210efc9", "status": "CLAIMED" }
  ]
}
```

**After completion** — the histogram flips to all `COMPLETED`:

```json
{
  "jobExecutionId": 1,
  "masterNode": "node-d3706bd7-0d54-4011-aa53-49ec6395ab53",
  "coordinationStatus": "COMPLETED",
  "partitionCount": 4,
  "statusCounts": { "COMPLETED": 4 },
  "partitions": [ "… four partitions, each status COMPLETED …" ]
}
```

| Field | Meaning |
|---|---|
| `masterNode` | the node that launched (and coordinates) this job execution |
| `partitionCount` | total partitions the master created for the step |
| `statusCounts` | histogram over partition status: `PENDING`, `CLAIMED`, `COMPLETED`, `FAILED` |
| `partitions[].assignedNode` | the node a partition is assigned to **right now** |
| `partitions[].status` | that partition's lifecycle state |

Because `assignedNode` reflects the *current* owner, this endpoint is also how you **watch failover**:
when a worker is lost, its transferable partitions reappear here assigned to a surviving node (and the
`statusCounts` momentarily shows them back in `PENDING`/`CLAIMED` before completing).

---

## Programmatic access — `BatchClusterQueryService`

The two job endpoints are thin wrappers over a public, read-only service bean. Inject it to build your
own API, UI, or metrics on the same stable read-model DTOs — no need to go through actuator:

```java
@Autowired
BatchClusterQueryService queryService;

List<JobSummaryView> jobs = queryService.listCoordinatedJobs();
Optional<JobClusterView> view = queryService.getJobView(jobExecutionId);
```

`JobSummaryView`, `JobClusterView`, and `PartitionView` are the same records serialized by the
endpoints above. The service issues plain read-only `SELECT`/`GROUP BY` queries and is scoped to live
and recent jobs (full history/retention is out of scope by design — see the roadmap).

---

## Security

!!! warning "Restrict exposure"
    These endpoints reveal operational detail — node hosts, partition assignments, job coordination
    state. Treat them like any other actuator endpoint: expose only what you need
    (`management.endpoints.web.exposure.include`), and protect them with authentication/authorization.
    Never expose `batch-cluster` / `batch-cluster-jobs` unauthenticated on a public interface. A common
    pattern is a separate, firewalled management port (`management.server.port`) reachable only from
    your ops network. See [Security](security.md) for the full hardening guidance.
