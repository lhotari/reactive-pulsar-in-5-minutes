package com.example.reactivepulsardemo;

import com.github.lhotari.reactive.pulsar.adapter.*;
import org.apache.pulsar.client.api.*;
import org.springframework.http.codec.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;

@RestController
public class EventFireHoseController {
   private final ReactiveMessageReader<TelemetryEvent> reactiveMessageReader;

   public EventFireHoseController(ReactivePulsarClient reactivePulsarClient) {
       reactiveMessageReader = reactivePulsarClient
               .messageReader(Schema.JSON(TelemetryEvent.class))
               .topic("telemetry_ingest")
               .startAtSpec(StartAtSpec.ofEarliest())
               .endOfStreamAction(EndOfStreamAction.POLL)
               .build();
   }

   @GetMapping("/firehose")
   public Flux<ServerSentEvent<TelemetryEvent>> firehose() {
       return reactiveMessageReader
               .readMessages()
               .map(message -> ServerSentEvent.builder(message.getValue())
                       .id(message.getMessageId().toString())
                       .build());
   }
}
