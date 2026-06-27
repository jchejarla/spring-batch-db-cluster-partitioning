/**
 * Per-database {@code DBSpecificQueryProvider} implementations.
 *
 * <p>Supports PostgreSQL, MySQL/MariaDB, Oracle, SQL Server, Db2, and H2. Only dialect-specific SQL
 * lives here — chiefly the heartbeat-age timestamp arithmetic and the unreachable-node sweeps; the
 * remaining queries are shared standard SQL defined on the provider interface.</p>
 */
package io.github.jchejarla.springbatch.clustering.core.serviceimpl;
