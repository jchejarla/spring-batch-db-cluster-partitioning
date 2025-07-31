package io.github.jchejarla.springbatch.clustering;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class BaseUnitTest {

    private AutoCloseable closeable;

    @BeforeEach
    protected void setup() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    protected void close() throws Exception {
        closeable.close();
    }
}
