package examples.io.github.jchejarla.springbatch.clustering;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Appends a short hex suffix to {@code spring.batch.cluster.node-id} when the
 * {@code h2} profile is active, so consecutive demo restarts do not collide on
 * the unique-key constraint in {@code BATCH_NODES}.
 *
 * <p>The bundled H2 demo profile keeps the database file across runs (so a
 * reviewer can inspect job and partition history with the H2 console). When a
 * node is force-killed, its row in {@code BATCH_NODES} persists until other
 * live nodes run the periodic cleanup (~75 seconds in the default config). If
 * the reviewer kills all nodes and restarts the same {@code java -jar} command
 * immediately, the new JVM tries to register with the same {@code node-id} as
 * the dead row and hits an integrity violation.
 *
 * <p>Rather than ask reviewers to wait or manually clean up state, this listener
 * silently appends an 8-character hex suffix so each JVM gets a unique node-id
 * (e.g. {@code worker-1} becomes {@code worker-1-3a8c1f2e}). The user-friendly
 * prefix stays at the front of the log so {@code worker-1} vs {@code worker-2}
 * is still easy to distinguish in the {@code >>>} lifecycle output.
 *
 * <p>The listener only runs when:
 * <ul>
 *   <li>the {@code h2} profile is active,</li>
 *   <li>the user explicitly set {@code spring.batch.cluster.node-id} (so the
 *       profile's own default UUID-based value is left alone).</li>
 * </ul>
 *
 * <p>Other profiles ({@code postgres}, {@code mysql}, {@code oracle}) are
 * unaffected. The listener is registered programmatically from
 * {@code StartApp.main()} so it does not depend on classpath discovery of
 * a {@code META-INF/spring/...imports} file, which is unreliable inside a
 * Spring Boot fat jar.
 */
public class H2NodeIdSuffixListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final String NODE_ID_PROPERTY = "spring.batch.cluster.node-id";
    private static final String H2_PROFILE = "h2";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        if (!isH2ProfileActive(environment)) {
            return;
        }
        String baseNodeId = environment.getProperty(NODE_ID_PROPERTY);
        if (baseNodeId == null || baseNodeId.isBlank()) {
            return;
        }
        if (isAlreadyUniquified(baseNodeId)) {
            // Either we have already added the suffix on a previous pass, or the
            // user provided a value that already contains random.uuid / a UUID —
            // do not stack suffixes on top.
            return;
        }

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String uniqueNodeId = baseNodeId + "-" + suffix;

        Map<String, Object> override = new HashMap<>();
        override.put(NODE_ID_PROPERTY, uniqueNodeId);
        environment.getPropertySources().addFirst(
                new MapPropertySource("h2NodeIdSuffix", override));

        System.out.println(">>> [startup] h2 profile: node-id "
                + baseNodeId + " -> " + uniqueNodeId);
    }

    private static boolean isH2ProfileActive(ConfigurableEnvironment env) {
        for (String profile : env.getActiveProfiles()) {
            if (H2_PROFILE.equalsIgnoreCase(profile)) {
                return true;
            }
        }
        for (String profile : env.getDefaultProfiles()) {
            if (H2_PROFILE.equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A "uniquified" id matches our own suffix shape (ends with {@code -<8 hex>})
     * or contains an embedded UUID (e.g. the profile's default
     * {@code ${HOSTNAME:node}-${random.uuid}} value, which is already unique).
     */
    private static boolean isAlreadyUniquified(String value) {
        if (value.length() >= 9) {
            String tail = value.substring(value.length() - 9);
            if (tail.charAt(0) == '-' && isHex(tail.substring(1))) {
                return true;
            }
        }
        return value.matches(".*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}.*");
    }

    private static boolean isHex(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }
}
