# Getting started: Reactive applications with Spring Boot and Pulsar

The goal of this guide is to get you started with building reactive applications with Spring Boot and Apache Pulsar in 5 minutes. 

* To learn more about "Reactive application with Spring Boot", go to https://spring.io/reactive
* For more information about “Reactive Pulsar” go to https://github.com/datastax/reactive-pulsar

Hint: You can follow this guide to build the application from scratch on your own. When using IntelliJ IDE, there's a handy feature which helps creating the Java classes. You can copy the source code snippets to the clipboard and paste directly to a Java package in IntelliJ. IntelliJ will automatically create a .java file which matches the Java class name in the pasted content.

### Questions or Feedback?

If you have any questions or feedback you can email PulsarQuestions@datastax.com .

## Step 1: Create a new Spring Reactive Web application with start.spring.io and add “Reactive Pulsar” dependencies

1. Go to https://start.spring.io/ 
2. Choose "Gradle project".
3. Fill in name "reactivepulsardemo"
4. Choose “Spring Reactive Web” and “Lombok” in dependencies
5. Click “Generate”
6. Extract zip file to a directory and open the generated app in an IDE
7. Edit the build.gradle file and add “Reactive Pulsar” to the dependencies block:
```
implementation 'com.github.lhotari:reactive-pulsar-spring-boot-starter:0.2.0'
testImplementation 'com.github.lhotari:reactive-pulsar-spring-test-support:0.2.0'
```

For more information about “Reactive Pulsar” go to https://github.com/datastax/reactive-pulsar .

### Step 2: Create a IoT backend application

Create these classes in the `com.example.reactivepulsardemo` package. In IntelliJ, you can create the class files by copy-pasting code from the clipboard directly to a Java package.

### Step 2a: Value object class for Telemetry events

```java
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TelemetryEvent {
    String n;
    double v;
}
```

### Step 2b: Ingestion REST controller for Telemetry events

```java
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
```

### Step 2c: Server Sent Event (SSE) controller for streaming telemetry

```java
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
```

## Step 3: Run the application and stream 1 million telemetry events

If you haven't built the application using the above steps, you can also get a copy with this commands and then continue the demonstration:
```shell
git clone https://github.com/lhotari/reactive-pulsar-in-5-minutes
cd reactive-pulsar-in-5-minutes
```

The assumption for the steps below is that you are using a bash shell with docker and curl available.

## Step 3a: Start local standalone Pulsar broker

In a terminal, execute this command:
```shell
docker run --rm -it -p 6650:6650 apachepulsar/pulsar:2.10.1 /pulsar/bin/pulsar standalone -nss -nfw
```

### Step 3b: Start the application

```shell
./gradlew bootRun
```

### Step 3c: Start consuming Server Sent Events with curl

```shell
curl -N localhost:8080/firehose
```

### Step 3d: Produce 1 million telemetry events with curl

```shell
# for bash shell
{ for i in {1..1000000}; do echo '{"n": "device'$i'/sensor1", "v": '$i'.123}'; done; } \
  | curl -X POST -T - -H "Content-Type: application/x-ndjson" localhost:8080/telemetry
```
