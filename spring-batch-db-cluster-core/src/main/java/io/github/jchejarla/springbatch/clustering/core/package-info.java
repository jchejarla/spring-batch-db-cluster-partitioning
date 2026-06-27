/**
 * The database-backed coordination core.
 *
 * <p>{@code DatabaseBackedClusterService} performs all coordination reads and writes — node
 * registration, heartbeats, partition lifecycle transitions, orphan detection, and master-job
 * recovery — through a {@code DBSpecificQueryProvider}, which isolates the small amount of
 * dialect-specific SQL from the shared standard SQL.</p>
 */
package io.github.jchejarla.springbatch.clustering.core;
