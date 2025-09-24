package com.example.reloader.service;

import com.example.reloader.config.ExternalDbConfig;
import com.example.reloader.entity.SenderQueueEntry;
import com.example.reloader.repository.SenderQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SenderServiceTest {
    private SenderQueueRepository repo;
    private ExternalDbConfig dummyConfig;
    private SenderService service;

    @BeforeEach
    void setup() {
        repo = Mockito.mock(SenderQueueRepository.class);
        dummyConfig = Mockito.mock(ExternalDbConfig.class);
        service = new SenderService(repo, dummyConfig);
    }

    @Test
    void enqueuePayloads_should_add_and_return_count() {
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        int added = service.enqueuePayloads(1, Arrays.asList("A","B"," ",null), "TEST");

        assertThat(added).isEqualTo(2);
        ArgumentCaptor<SenderQueueEntry> captor = ArgumentCaptor.forClass(SenderQueueEntry.class);
        verify(repo, times(2)).save(captor.capture());
        List<SenderQueueEntry> saved = captor.getAllValues();
        assertThat(saved.get(0).getPayloadId()).isEqualTo("A");
        assertThat(saved.get(1).getPayloadId()).isEqualTo("B");
    }

    @Test
    void processBatch_should_mark_sent() {
        SenderQueueEntry e1 = new SenderQueueEntry(1, "P1", "T");
        e1.setStatus("NEW");
        when(repo.findByStatusOrderByCreatedAt(eq("NEW"), any())).thenReturn(Collections.singletonList(e1));

        service.processBatch(10);

        // after processing it should be saved twice: PROCESSING then SENT
        verify(repo, atLeast(2)).save(any(SenderQueueEntry.class));
        assertThat(e1.getStatus()).isEqualTo("SENT");
        assertThat(e1.getProcessedAt()).isNotNull();
    }

    @Test
    void getQueue_delegates_to_repo() {
        when(repo.findBySenderIdAndStatusOrderByCreatedAt(eq(2), eq("NEW"), any())).thenReturn(Collections.emptyList());
        List<SenderQueueEntry> result = service.getQueue(2, "NEW", 5);
        assertThat(result).isEmpty();
        verify(repo).findBySenderIdAndStatusOrderByCreatedAt(eq(2), eq("NEW"), any());
    }
}
