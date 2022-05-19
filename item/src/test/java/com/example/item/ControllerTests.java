package com.example.item;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.concurrent.TimeUnit;
import com.example.grpc.item.ItemOuterClass.CreateReply;
import com.example.grpc.item.ItemOuterClass.CreateRequest;
import com.example.grpc.item.ItemOuterClass.Status;
import com.google.cloud.firestore.Firestore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.grpc.internal.testing.StreamRecorder;

public class ControllerTests {

  private Controller controller;
  private Firestore firestore;

  @BeforeEach
  public void setup() {
    controller = new Controller(firestore);
  }

  @Test
  void outputTest() throws Exception {
    CreateRequest request = CreateRequest.newBuilder().setName("Test").build();
    StreamRecorder<CreateReply> responseObserver = StreamRecorder.create();
    controller.create(request, responseObserver);
    if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
      fail("The call did not terminate in time");
    }
    assertNull(responseObserver.getError());
    var results = responseObserver.getValues();
    assertEquals(1, results.size());
    var response = results.get(0);
    assertEquals(CreateReply.newBuilder().setStatus(Status.PENDING).build(), response);
  }

}
