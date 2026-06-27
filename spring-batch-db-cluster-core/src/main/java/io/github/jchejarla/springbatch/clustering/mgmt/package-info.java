/**
 * Cluster membership and lifecycle management.
 *
 * <p>Covers node registration and periodic heartbeats, the two-phase node lifecycle
 * (active &rarr; unreachable &rarr; removed), and recovery of jobs whose master node was lost so a
 * stranded execution becomes cleanly restartable rather than hanging.</p>
 */
package io.github.jchejarla.springbatch.clustering.mgmt;
