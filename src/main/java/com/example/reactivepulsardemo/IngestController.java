package com.example.reactivepulsardemo;

import com.github.lhotari.reactive.pulsar.adapter.*;
import com.github.lhotari.reactive.pulsar.resourceadapter.*;
import org.apache.pulsar.client.api.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;

@RestController
public class IngestController {
   private final ReactiveMessageSender<TelemetryEvent> reactiveMessageSender;

   public IngestController(ReactivePulsarClient reactivePulsarClient,
                           ReactiveProducerCache reactiveProducerCache) {
       reactiveMessageSender = reactivePulsarClient
               .messageSender(Schema.JSON(TelemetryEvent.class))
               .topic("telemetry_ingest")
               .cache(reactiveProducerCache)
               .maxInflight(100)
               .build();
   }

   @PostMapping("/telemetry")
   public Mono<Void> ingestTelemetry(@RequestBody Flux<TelemetryEvent> telemetryEventFlux) {
       return reactiveMessageSender
               .sendMessages(telemetryEventFlux.map(MessageSpec::of))
               .then();
   }
}