package dev.jchejarla.springbatch.clustering.autoconfigure.conditions;

import dev.jchejarla.springbatch.clustering.BaseUnitTest;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class OnClusterEnabledConditionUnitTest extends BaseUnitTest {

    @Spy
    OnClusterEnabledCondition onClusterEnabledCondition;

    @Test
    public void testMatches() {
        ConditionContext context = mock(ConditionContext.class);
        Environment environment = mock(Environment.class);
        doReturn(environment).when(context).getEnvironment();
        doReturn(true).when(environment).getProperty(anyString(), any(Class.class), anyBoolean());
        boolean isEnabled = onClusterEnabledCondition.matches(context, mock(AnnotatedTypeMetadata.class));
        assertTrue(isEnabled);
        doReturn(false).when(environment).getProperty(anyString(), any(Class.class), anyBoolean());
        isEnabled = onClusterEnabledCondition.matches(context, mock(AnnotatedTypeMetadata.class));
        assertFalse(isEnabled);
    }
}
