/**
 * Spring Boot auto-configuration and the configuration-property surface for clustering.
 *
 * <p>{@code BatchClusterProperties} binds the {@code spring.batch.cluster.*} namespace, and the
 * auto-configuration wires the coordination service, partition handler, node manager, recovery
 * manager, worker runner, schedulers, and actuator beans. Everything here activates only when
 * {@code spring.batch.cluster.enabled=true}.</p>
 */
package io.github.jchejarla.springbatch.clustering.autoconfigure;
