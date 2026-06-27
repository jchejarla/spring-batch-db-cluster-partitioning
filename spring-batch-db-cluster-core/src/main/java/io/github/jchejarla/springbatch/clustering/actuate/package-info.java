/**
 * Spring Boot Actuator integration for observability of the batch cluster.
 *
 * <p>Provides a {@code /actuator/batch-cluster} endpoint exposing nodes and their partition
 * assignments, health indicators for overall cluster and per-node liveness, and an info contributor
 * that reports the active clustering configuration.</p>
 */
package io.github.jchejarla.springbatch.clustering.actuate;
