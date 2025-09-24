package com.example.reloader.web;

import com.example.reloader.entity.SenderQueueEntry;
import com.example.reloader.service.SenderService;
import com.example.reloader.web.dto.EnqueueRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SenderController.class)
class SenderControllerTest {
    @Autowired
    MockMvc mvc;

    @MockBean
    SenderService service;
    
    @MockBean
    com.example.reloader.repository.SenderQueueRepository repo;

    @BeforeEach
    void setup() {
    }

    @Test
    void getQueue_endpoint_returns_json() throws Exception {
    when(service.getQueue(anyInt(), org.mockito.ArgumentMatchers.eq("NEW"), org.mockito.ArgumentMatchers.anyInt())).thenReturn(Arrays.asList(new SenderQueueEntry()));
        mvc.perform(get("/api/senders/1/queue").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void enqueue_endpoint_delegates() throws Exception {
        EnqueueRequest req = new EnqueueRequest();
        req.setSenderId(1);
        req.setPayloadIds(Arrays.asList("X","Y"));
        ObjectMapper om = new ObjectMapper();

    when(service.enqueuePayloads(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyString())).thenReturn(2);

        mvc.perform(post("/api/senders/1/enqueue").contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Enqueued")));
    }
}
