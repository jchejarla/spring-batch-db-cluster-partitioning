# Contributing to spring-batch-db-cluster-partitioning

Thank you for your interest in contributing! This guide explains how to report issues, suggest improvements, and submit code changes.

## Reporting Bugs

1. **Search existing issues** — check the [issue tracker](https://github.com/jchejarla/spring-batch-db-cluster-partitioning/issues) to see if the bug has already been reported.
2. **Open a new issue** using the *Bug Report* template.
3. Include at minimum:
   - A clear title and description of the problem.
   - Steps to reproduce (minimal code snippet, configuration, or example project).
   - Expected vs actual behaviour.
   - Environment details: Java version, Spring Boot version, database type and version, OS.
   - Stack traces or log output (wrap in a code block).

## Suggesting Features or Enhancements

Open a new issue with the *Feature Request* label. Describe the use case, the expected behaviour, and (if possible) how you would approach the implementation. Discussion is welcome before coding begins.

## Development Setup

### Prerequisites

- Java 17 or later
- Maven 3.x
- A relational database for integration tests (PostgreSQL recommended; H2 works for unit tests)
- Git

### Building Locally

```bash
# Clone your fork
git clone https://github.com/<your-username>/spring-batch-db-cluster-partitioning.git
cd spring-batch-db-cluster-partitioning

# Build and run all tests
mvn clean verify

# Run only unit tests (no database required)
mvn test

# Run integration tests (requires a running database — see src/test/resources/application-it.yml)
mvn verify -Pit
```

### Project Structure

```
spring-batch-db-cluster-core/    Core library (the published Maven artifact)
  src/main/java/                 Production source code
  src/test/java/                 Unit and integration tests
examples/                        Runnable example application
docs/                            Architecture diagrams, design documents
```

## Submitting a Pull Request

1. **Fork** the repository and create a feature branch from `main`:
   ```bash
   git checkout -b feature/my-improvement
   ```
2. **Write your code.** Follow the existing code style (standard Java conventions, meaningful names, Javadoc on public APIs).
3. **Add or update tests.** Every bug fix should include a regression test; every new feature should include unit tests and, where appropriate, integration tests.
4. **Run the full build** to make sure nothing is broken:
   ```bash
   mvn clean verify
   ```
5. **Commit** with a clear, descriptive message:
   ```bash
   git commit -m "Add round-robin rebalance on node join"
   ```
6. **Push** to your fork and open a pull request against `main`.
7. In the PR description, reference the related issue (e.g., `Closes #42`) and describe what changed and why.

### Pull Request Review

- A maintainer will review your PR, possibly request changes, and merge once approved.
- CI must pass (build + tests) before merging.
- Please keep PRs focused — one logical change per PR.

## Code Style

- Standard Java naming conventions (camelCase for methods/variables, PascalCase for classes).
- Use `final` for fields and parameters where practical.
- Prefer clear code over clever code; add comments only where the *why* is not obvious from the code itself.
- All public classes and methods should have Javadoc.

## Testing Guidelines

- **Unit tests** use JUnit 5 and Mockito. Place them in `src/test/java` with a `*UnitTest.java` suffix.
- **Integration tests** run against a real database. Place them in `src/test/java` with a `*IntegrationTest.java` suffix.
- Tests should be deterministic and not depend on execution order.

## License

By contributing, you agree that your contributions will be licensed under the [Apache License 2.0](LICENSE), the same license that covers this project.

## Questions?

If you have questions about contributing, open a [Discussion](https://github.com/jchejarla/spring-batch-db-cluster-partitioning/discussions) or reach out via the issue tracker.
