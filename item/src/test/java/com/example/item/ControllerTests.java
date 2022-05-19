package com.example.item;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.example.grpc.item.ItemOuterClass.CreateReply;
import com.example.grpc.item.ItemOuterClass.CreateRequest;
import com.example.grpc.item.ItemOuterClass.Status;
import com.example.item.Bean.Items;
import com.google.api.core.ApiFuture;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import io.grpc.internal.testing.StreamRecorder;

// @SpringBootTest
// @ExtendWith(SpringExtension.class)
public class ControllerTests {

  private Controller controller;

  private Firestore firestore;

  private static final UUID uuid = UUID.fromString("5af48f3b-468b-4ae0-a065-7d7ac70b37a8");


  @BeforeEach
  public void setup() throws IOException {
    // mock の firestore 作成
    firestore = FirestoreOptions.getDefaultInstance().toBuilder().setProjectId("test")
        .setHost("localhost:9000").setCredentials(new FirestoreOptions.EmulatorCredentials())
        .setCredentialsProvider(
            FixedCredentialsProvider.create(new FirestoreOptions.EmulatorCredentials()))
        .build().getService();
    controller = new Controller(firestore);

    // initialize the mock of the static method
    MockedStatic<UUID> mock = Mockito.mockStatic(UUID.class);
    // define the behaviour of the mock
    mock.when(UUID::randomUUID).thenReturn(uuid);

  }

  @Test
  void outputTest() throws Exception {
    var itemName = "itemName";
    CreateRequest request = CreateRequest.newBuilder().setName(itemName).build();
    StreamRecorder<CreateReply> responseObserver = StreamRecorder.create();
    controller.create(request, responseObserver);
    if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
      fail("The call did not terminate in time");
    }
    assertNull(responseObserver.getError());
    var results = responseObserver.getValues();
    List<CreateReply> expected =
        Arrays.asList(CreateReply.newBuilder().setStatus(Status.PENDING).build(),
            CreateReply.newBuilder().setStatus(Status.FINISH).build());
    assertIterableEquals(expected, results);

    var strUuid = uuid.toString();
    ApiFuture<DocumentSnapshot> documentSnapshotApiFuture =
        firestore.document("items/" + strUuid).get();

    var item = documentSnapshotApiFuture.get().toObject(Items.class);;
    var expectedData = new Items(strUuid, itemName, Arrays.asList("testid"));

    assertEquals(expectedData, item);
  }

}
