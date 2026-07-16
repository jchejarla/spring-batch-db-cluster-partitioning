# Usage guide

This walks through wiring a partitioned job onto the cluster. It assumes you have already added the
dependency, enabled a JDBC job repository, and set `spring.batch.cluster.enabled=true` — see
[Installation](installation.md).

Your job and step definitions stay standard Spring Batch. Two things are cluster-specific: a
**`ClusterAwarePartitioner`** (how work is split and assigned), and wiring the manager step to the
library's **`ClusterAwarePartitionHandler`** and **`ClusterAwareAggregator`**.

## 1. Write a `ClusterAwarePartitioner`

Extend the abstract `ClusterAwarePartitioner` and implement three methods. The key one,
`createDistributedPartitions`, receives the **live node count** so you can size the split to the cluster
that actually exists right now.

!!! warning "The partitioner must be a Spring bean — don't `new` it"
    `ClusterAwarePartitioner` has the cluster service injected into it by the framework, so it only works
    as a **Spring-managed bean**. A plain `new RangeSumPartitioner(...)` is never injected and fails at
    runtime with a `NullPointerException`. Annotate it (`@Component` below, or declare it as a `@Bean`),
    and use `@StepScope` if it reads job parameters (as here).

```java
import io.github.jchejarla.springbatch.clustering.api.ClusterAwarePartitioner;
import io.github.jchejarla.springbatch.clustering.api.PartitionStrategy;
import io.github.jchejarla.springbatch.clustering.partition.PartitioningMode;
import io.github.jchejarla.springbatch.clustering.partition.PartitionTransferableProp;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope   // a new instance per job run, so it can read this run's job parameters
public class RangeSumPartitioner extends ClusterAwarePartitioner {

    private final long from, to;

    public RangeSumPartitioner(@Value("#{jobParameters['from']}") long from,
                               @Value("#{jobParameters['to']}") long to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public List<ExecutionContext> createDistributedPartitions(int availableNodeCount) {
        // Size the split to the live cluster; each ExecutionContext is one partition's input.
        int partitions = availableNodeCount * 2;
        long span = (to - from + 1) / partitions;
        List<ExecutionContext> result = new ArrayList<>(partitions);
        long start = from;
        for (int i = 0; i < partitions; i++) {
            long end = (i == partitions - 1) ? to : start + span - 1;
            ExecutionContext ctx = new ExecutionContext();
            ctx.putLong("start", start);
            ctx.putLong("end", end);
            result.add(ctx);
            start = end + 1;
        }
        return result;
    }

    @Override
    public PartitionTransferableProp arePartitionsTransferableWhenNodeFailed() {
        // YES: if a node dies, its incomplete partitions are reassigned to healthy nodes.
        // NO: partitions are node-local / non-idempotent — on node loss they are failed, not moved.
        return PartitionTransferableProp.YES;
    }

    @Override
    public PartitionStrategy buildPartitionStrategy() {
        return PartitionStrategy.builder()
                .partitioningMode(PartitioningMode.ROUND_ROBIN)
                .build();
    }
}
```

### Assignment strategies

`buildPartitionStrategy()` chooses how partitions map onto nodes:

| Mode | Behaviour |
|---|---|
| `ROUND_ROBIN` | Even distribution across all live nodes. |
| `FIXED_NODE_COUNT` | Restrict to *N* nodes — `PartitionStrategy.builder().partitioningMode(FIXED_NODE_COUNT).fixedNodeCount(3).build()`. |
| `LEAST_LOADED` | Load-aware — assigns each partition to the node with the lowest live load, steering work away from nodes already busy with other jobs. |

### Transferability

`arePartitionsTransferableWhenNodeFailed()` is a **correctness contract**, not a hint. `YES` means a lost
node's incomplete partitions are safe to re-run elsewhere (idempotent work). `NO` means they must never
be re-executed on another node — when the owning node is lost they are **failed** so the job fails
cleanly, rather than risking a double side effect. Choose deliberately.

## 2. Wire the partitioned step

The manager step uses your partitioner plus the library-provided `ClusterAwarePartitionHandler`; the
worker step is an ordinary Spring Batch step that each node runs for its assigned partitions.

```java
@Configuration
public class RangeSumJobConfig {

    @Autowired ClusterAwarePartitionHandler partitionHandler;   // provided by auto-configuration
    @Autowired RangeSumPartitioner rangeSumPartitioner;         // the @Component bean from step 1

    @Bean
    public Job rangeSumJob(JobRepository jobRepository, PlatformTransactionManager txnManager,
                           StepExecutionAggregator aggregator) {
        return new JobBuilder("rangeSumJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(managerStep(jobRepository, txnManager, aggregator))
                .build();
    }

    public Step managerStep(JobRepository jobRepository, PlatformTransactionManager txnManager,
                            StepExecutionAggregator aggregator) {
        return new StepBuilder("rangeSumStep.manager", jobRepository)
                // inject the partitioner BEAN — never `new RangeSumPartitioner(...)` (see step 1).
                .partitioner("rangeSumStep", rangeSumPartitioner)
                .partitionHandler(partitionHandler)     // ← distributes via the database
                .step(rangeSumStep(jobRepository, txnManager))
                .aggregator(aggregator)
                .build();
    }

    // The worker step. Its BEAN NAME must equal the step name and the name passed to
    // .partitioner("rangeSumStep", ...) above — worker nodes look the step up by that name in the
    // application context. Here the @Bean method name, the StepBuilder name, and the partitioner name
    // are all "rangeSumStep"; if they diverge, workers fail with NoSuchBeanDefinitionException.
    @Bean
    public Step rangeSumStep(JobRepository jobRepository, PlatformTransactionManager txnManager) {
        return new StepBuilder("rangeSumStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    ExecutionContext ctx = chunkContext.getStepContext()
                            .getStepExecution().getExecutionContext();
                    long start = ctx.getLong("start"), end = ctx.getLong("end");
                    long sum = 0;
                    for (long i = start; i <= end; i++) sum += i;
                    ctx.putLong("result", sum);
                    return RepeatStatus.FINISHED;
                }, txnManager)
                .build();
    }
}
```

Nothing about the worker step is cluster-aware — it just reads its partition's `ExecutionContext` and
does the work. Distribution, claiming, and failover happen underneath, in `ClusterAwarePartitionHandler`.
For a **chunk-oriented ETL** worker (a `@StepScope` reader/processor/writer reading `start`/`end` from
`#{stepExecutionContext['start']}`), see the CSV→XML job in [Examples](examples.md).

!!! note "Spring Batch 6 import"
    `RunIdIncrementer` moved packages in Batch 6 — import it from
    `org.springframework.batch.core.job.parameters.RunIdIncrementer` (it was in `…launch.support` in
    Batch 5, and the old path no longer compiles).

## 3. Aggregate the results

`ClusterAwareAggregator` collects the completed partitions on the master and invokes your callback.
Provide a `ClusterAwareAggregatorCallback` for the success/failure hooks:

```java
@Bean
public StepExecutionAggregator aggregator(JobRepository jobRepository) {
    return new ClusterAwareAggregator(new ClusterAwareAggregatorCallback() {
        @Override
        public void onSuccess(Collection<StepExecution> executions) {
            long total = executions.stream()
                    .map(se -> se.getExecutionContext().get("result", Long.class))
                    .filter(Objects::nonNull).mapToLong(Long::longValue).sum();
            log.info("Job complete — total = {}", total);
        }

        @Override
        public void onFailure(Collection<StepExecution> executions) {
            log.error("Job failed — some partitions did not complete");
        }
    }, jobRepository);
}
```

## 4. Run more than one instance

Start two or more instances of your Spring Boot application (different hosts, or different ports on one
machine). Each node registers itself and generates a unique id automatically — no per-instance config.
Launch the job on any node: that node becomes the **master** for that execution and the rest act as
workers. Two jobs launched on two nodes have two independent masters, concurrently.

!!! warning "Don't let the job auto-run at startup"
    If you have a `Job` bean and launch jobs on demand (REST, a scheduler, …), set
    **`spring.batch.job.enabled=false`**. Otherwise Spring Boot's `JobLauncherApplicationRunner` runs the
    job at application startup — before the cluster has formed — and it fails. Trigger jobs yourself once
    the app is up.

Watch it run through the [actuator endpoints](Observability.md), and see a complete, runnable
multi-node project — including a failover walkthrough — in [Examples](examples.md).
