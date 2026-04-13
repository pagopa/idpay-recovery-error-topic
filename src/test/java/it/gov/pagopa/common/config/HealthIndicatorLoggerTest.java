package it.gov.pagopa.common.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import it.gov.pagopa.common.utils.MemoryAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthIndicatorLoggerTest {

    private MemoryAppender memoryAppender;
    private HealthIndicatorLogger healthIndicatorLogger;

    private HealthIndicator diskSpaceIndicator;
    private HealthIndicator dbIndicator;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(HealthIndicatorLogger.class);
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        memoryAppender.setName("MEMORY");
        memoryAppender.start();

        logger.setLevel(Level.INFO);
        logger.addAppender(memoryAppender);

        diskSpaceIndicator = Mockito.mock(HealthIndicator.class);
        dbIndicator = Mockito.mock(HealthIndicator.class);

        healthIndicatorLogger = new HealthIndicatorLogger(
                List.of(diskSpaceIndicator, dbIndicator)
        );
    }

    @Test
    void shouldLogWhenStatusIsDown() {
        Mockito.when(diskSpaceIndicator.health())
                .thenReturn(Health.down()
                        .withDetails(Map.of("DETAILKEY", "DETAILVALUE"))
                        .build());

        Mockito.when(dbIndicator.health())
                .thenReturn(Health.up().build());

        Health result = healthIndicatorLogger.health();

        assertEquals(Status.UP, result.getStatus());
        assertEquals(1, memoryAppender.getLoggedEvents().size());
    }

    @Test
    void shouldLogWhenStatusIsOutOfService() {
        Mockito.when(diskSpaceIndicator.health())
                .thenReturn(Health.outOfService()
                        .withDetails(Map.of("DETAILKEY", "DETAILVALUE"))
                        .build());

        Mockito.when(dbIndicator.health())
                .thenReturn(Health.up().build());

        Health result = healthIndicatorLogger.health();

        assertEquals(Status.UP, result.getStatus());
        assertEquals(1, memoryAppender.getLoggedEvents().size());
    }

    @Test
    void shouldNotLogWhenAllIndicatorsAreUp() {
        Mockito.when(diskSpaceIndicator.health())
                .thenReturn(Health.up().build());

        Mockito.when(dbIndicator.health())
                .thenReturn(Health.up().build());

        Health result = healthIndicatorLogger.health();

        assertEquals(Status.UP, result.getStatus());
        assertEquals(0, memoryAppender.getLoggedEvents().size());
    }
}