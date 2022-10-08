/*
 * Copyright 2022 Yurii Serhiichuk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.cloudevents.example.client;

import com.google.protobuf.TextFormat;
import io.cloudevents.example.proto.GreeterGrpc;
import io.cloudevents.v1.proto.CloudEvent;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple client that requests a greeting from the server.
 */
public class Client {
  private static final Logger logger = Logger.getLogger(Client.class.getName());

  private final GreeterGrpc.GreeterBlockingStub blockingStub;

  /**
   * Construct client for accessing HelloWorld server using the existing channel.
   */
  public Client(Channel channel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to reuse Channels.
    blockingStub = GreeterGrpc.newBlockingStub(channel);
  }

  /**
   * Say hello to server.
   */
  public void greet() {
    CloudEvent request = CloudEvent.newBuilder()
        .setType("io.cloudevents.example.client.RequestSent")
        .setSource("io.cloudevents.example.client")
        .setSpecVersion("1.0.0")
        .setId("1")
        .setTextData("ping")
        .build();
    logger.info("Sending CloudEvent: " + TextFormat.shortDebugString(request));
    CloudEvent response;
    try {
      response = blockingStub.hello(request);
    } catch (StatusRuntimeException e) {
      logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
      return;
    }
    logger.info("Received response: " + TextFormat.shortDebugString(response));
  }

  /**
   * Greet server. If provided, the first element of {@code args} is the name to use in the
   * greeting. The second argument is the target server.
   */
  public static void main(String[] args) throws Exception {
    // Access a service running on the local machine on port 50051
    String target = "localhost:52051";
    // Create a communication channel to the server, known as a Channel. Channels are thread-safe
    // and reusable. It is common to create channels at the beginning of your application and reuse
    // them until the application shuts down.
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
        // needing certificates.
        .usePlaintext()
        .build();
    try {
      Client client = new Client(channel);
      client.greet();
    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
      // resources the channel should be shut down when it will no longer be used. If it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
