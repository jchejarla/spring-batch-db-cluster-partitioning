/*
 * Copyright 2025 Janardhan Chejarla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.jchejarla.springbatch.clustering.autoconfigure.conditions;

import io.github.jchejarla.springbatch.clustering.BaseUnitTest;
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
