package examples.io.github.jchejarla.springbatch.clustering;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Appends a short hex suffix to {@code spring.batch.cluster.node-id} for the
 * bundled demo profiles, so consecutive restarts after a hard kill do not
 * collide on the unique-key constraint in {@code BATCH_NODES}.
 *
 * <p>Active only under the {@code h2} or {@code postgres} profile. The
 * {@code mysql} and {@code oracle} profiles are unaffected.
 */
public class DemoNodeIdSuffixListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final String NODE_ID_PROPERTY = "spring.batch.cluster.node-id";
    private static final Set<String> DEMO_PROFILES = Set.of("h2", "postgres");

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        String activeProfile = findActiveDemoProfile(environment);
        if (activeProfile == null) {
            return;
        }
        String baseNodeId = environment.getProperty(NODE_ID_PROPERTY);
        if (baseNodeId == null || baseNodeId.isBlank()) {
            return;
        }
        if (isAlreadyUniquified(baseNodeId)) {
            return;
        }

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String uniqueNodeId = baseNodeId + "-" + suffix;

        Map<String, Object> override = new HashMap<>();
        override.put(NODE_ID_PROPERTY, uniqueNodeId);
        environment.getPropertySources().addFirst(
                new MapPropertySource("demoNodeIdSuffix", override));

        System.out.println(">>> [startup] " + activeProfile + " profile: node-id "
                + baseNodeId + " -> " + uniqueNodeId);
    }

    private static String findActiveDemoProfile(ConfigurableEnvironment env) {
        for (String profile : env.getActiveProfiles()) {
            if (DEMO_PROFILES.contains(profile.toLowerCase())) {
                return profile.toLowerCase();
            }
        }
        for (String profile : env.getDefaultProfiles()) {
            if (DEMO_PROFILES.contains(profile.toLowerCase())) {
                return profile.toLowerCase();
            }
        }
        return null;
    }

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
