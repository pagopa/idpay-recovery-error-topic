package it.gov.pagopa.idpay.error_recovery.consumer;

import it.gov.pagopa.idpay.error_recovery.service.ErrorMessageMediatorService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import static org.mockito.Mockito.*;

class ErrorMessagesListenerTest {

    private ErrorMessageMediatorService mediatorService;
    private ConcurrentMessageListenerContainer<String, String> container;
    private Acknowledgment acknowledgment;

    private ErrorMessagesListener listener;

    @BeforeEach
    void setUp() {
        mediatorService = mock(ErrorMessageMediatorService.class);
        container = mock(ConcurrentMessageListenerContainer.class);
        acknowledgment = mock(Acknowledgment.class);

        listener = new ErrorMessagesListener(
                5L,
                container,
                mediatorService
        );
    }

    @Test
    void shouldPauseContainerAndProcessMessages() {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("topic", 0, 0L, "key", "value");

        listener.onMessage(List.of(record), acknowledgment);

        verify(container, times(1)).pause();
        verify(mediatorService, times(1)).accept(record);
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void shouldHandleExceptionWithoutFailing() {
        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("topic", 0, 0L, "key", "value");

        doThrow(new RuntimeException("boom"))
                .when(mediatorService).accept(record);

        listener.onMessage(List.of(record), acknowledgment);

        verify(container, times(1)).pause();
        verify(mediatorService, times(1)).accept(record);

        // must still ack even if exception occurs
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void shouldResumeContainerWhenPauseTimeElapsed() throws Exception {
        listener = new ErrorMessagesListener(
                1L,
                container,
                mediatorService
        );

        // simulate past pause
        listener.onMessage(List.of(
                new ConsumerRecord<>("t", 0, 0L, "k", "v")
        ), acknowledgment);

        // force wait to exceed pause
        Thread.sleep(1100);

        listener.scheduledResume();

        verify(container, times(1)).resume();
    }

    @Test
    void shouldNotResumeIfPauseTimeNotElapsed() {
        listener = new ErrorMessagesListener(
                1000L,
                container,
                mediatorService
        );

        listener.scheduledResume();

        verify(container, never()).resume();
    }

    @Test
    void shouldProcessMultipleRecords() {
        ConsumerRecord<String, String> r1 =
                new ConsumerRecord<>("topic", 0, 0L, "k1", "v1");
        ConsumerRecord<String, String> r2 =
                new ConsumerRecord<>("topic", 0, 1L, "k2", "v2");

        listener.onMessage(List.of(r1, r2), acknowledgment);

        ArgumentCaptor<ConsumerRecord<String, String>> captor =
                ArgumentCaptor.forClass(ConsumerRecord.class);

        verify(mediatorService, times(2)).accept(captor.capture());

        assert captor.getAllValues().contains(r1);
        assert captor.getAllValues().contains(r2);
    }
}