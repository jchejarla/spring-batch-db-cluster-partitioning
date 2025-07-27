package io.github.jchejarla.springbatch.clustering.autoconfigure.conditions;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class OnClusterEnabledCondition implements Condition {

    public static final String CLUSTER_ENABLED = "spring.batch.cluster.enabled";

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return context.getEnvironment().getProperty(CLUSTER_ENABLED, Boolean.class, false);
    }
}
