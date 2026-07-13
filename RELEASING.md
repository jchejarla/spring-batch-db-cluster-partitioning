# Releasing

Release process for `io.github.jchejarla:spring-batch-db-cluster-core` to Maven Central
(via the Sonatype Central Portal).

## Prerequisites (one-time)

- `~/.m2/settings.xml` has a `<server>` with `<id>central</id>` and your Central Portal token
  (this id matches `publishingServerId` in the release profile).
- A GPG key is available and its passphrase is configured (key `B35BEB20…`, see `<gpg.keyname>` in the
  parent `pom.xml`). Central rejects unsigned artifacts.
- Docker is available if you run the cross-database dialect ITs locally (see step 2).

## Steps

1. **Green build.** `mvn clean install` — unit tests + the H2 integration test must pass.

2. **Validate the dialects** (SQL Server / Db2 / MariaDB run only under a flag and need Docker):
   ```bash
   mvn -pl spring-batch-db-cluster-core verify -Dcontainer.it=true -Dcontainer.it.db2=true
   ```
   These also run in CI via the **Dialect ITs** workflow. Confirm that workflow is green for the commit
   you are tagging.

3. **Set the version.** Bump `3.0.0-SNAPSHOT` → `3.0.0` in all three poms
   (`pom.xml`, `spring-batch-db-cluster-core/pom.xml`, `examples/pom.xml`), and date the
   `[3.0.0]` heading in `CHANGELOG.md` (replace `Unreleased`).

4. **Deploy to Central.** Use the **`release`** profile — it activates GPG signing and the
   Central-publishing plugin. (There is no `ossrh` profile; `-P ossrh` would skip signing and fail.)
   ```bash
   mvn clean deploy -pl spring-batch-db-cluster-core -am -P release -s ~/.m2/settings.xml
   ```
   `-am` builds the aggregator parent into the reactor so its signed `.pom` is bundled too (Central needs
   the parent POM for the core artifact's inherited metadata to resolve). `examples` is excluded from
   publishing by its own pom. Confirm the upload bundle contains: core `.jar`, `-sources.jar`,
   `-javadoc.jar`, each with a `.asc` signature, plus the signed parent `.pom`.

5. **Tag.** `git tag v3.0.0 && git push origin v3.0.0`. The tag triggers the docs workflow, which
   publishes the frozen `3.0.0` docs and moves the `latest` alias.

6. **Verify.** Confirm the artifact appears on Central and `…/docs/latest/` renders.

7. **Open the next dev cycle.** Bump the poms to the next `-SNAPSHOT` (e.g. `3.1.0-SNAPSHOT`).

## Notes

- The `<distributionManagement>` block in the parent pom points at the legacy OSSRH host
  (`s01.oss.sonatype.org`), which is decommissioned. The Central-publishing plugin ignores it on a
  `release` deploy, but a plain `mvn deploy` (or a SNAPSHOT deploy) would hit that dead endpoint —
  review/remove it before relying on non-`release` deploys.
- The `2.x` line stays on Spring Boot 3 for maintenance; release it from the `2.x` branch.
