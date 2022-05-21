package com.example.item;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import com.example.grpc.item.ItemOuterClass.Bean;
import com.example.grpc.item.ItemOuterClass.CreateReply;
import com.example.grpc.item.ItemOuterClass.CreateRequest;
import com.example.grpc.item.ItemOuterClass.SearchReply;
import com.example.grpc.item.ItemOuterClass.SearchRequest;
import com.example.grpc.item.ItemOuterClass.Status;
import com.example.grpc.item.ItemOuterClass.UpdateReply;
import com.example.grpc.item.ItemOuterClass.UpdateRequest;
import com.example.item.Bean.Items;
import com.google.api.core.ApiFuture;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
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
  private static final UUID uuidSearch1 = UUID.fromString("5af48f3b-468b-4ae0-a065-7d7ac70b37a9");
  private static final UUID uuidSearch2 = UUID.fromString("5af48f3b-468b-4ae0-a065-7d7ac70b37aA");
  private static final UUID uuidSearch3 = UUID.fromString("5af48f3b-468b-4ae0-a065-7d7ac70b37ab");

  @BeforeAll
  static public void init() {
    // initialize the mock of the static method
    MockedStatic<UUID> mock = Mockito.mockStatic(UUID.class);
    // define the behaviour of the mock
    mock.when(UUID::randomUUID).thenReturn(uuid);
  }

  /**
   * Delete a collection in batches to avoid out-of-memory errors. Batch size may be tuned based on
   * document size (atmost 1MB) and application requirements.
   */
  void deleteCollection(CollectionReference collection, int batchSize) {
    try {
      // retrieve a small batch of documents to avoid out-of-memory errors
      ApiFuture<QuerySnapshot> future = collection.limit(batchSize).get();
      int deleted = 0;
      // future.get() blocks on document retrieval
      List<QueryDocumentSnapshot> documents = future.get().getDocuments();
      for (QueryDocumentSnapshot document : documents) {
        document.getReference().delete();
        ++deleted;
      }
      if (deleted >= batchSize) {
        // retrieve and delete another batch
        deleteCollection(collection, batchSize);
      }
    } catch (Exception e) {
      System.err.println("Error deleting collection : " + e.getMessage());
    }
  }

  @BeforeEach
  public void setup() throws IOException {
    // mock の firestore 作成
    firestore = FirestoreOptions.getDefaultInstance().toBuilder().setProjectId("test")
        .setHost("localhost:9000").setCredentials(new FirestoreOptions.EmulatorCredentials())
        .setCredentialsProvider(
            FixedCredentialsProvider.create(new FirestoreOptions.EmulatorCredentials()))
        .build().getService();
    controller = new Controller(firestore);
  }

  @AfterEach
  public void closing() {
    deleteCollection(firestore.collection("items"), 100);
  }

  @Test
  void createTest() throws Exception {
    var itemName = "itemName";
    var ids = Arrays.asList("testid", "testid2");
    CreateRequest request = CreateRequest.newBuilder().setName(itemName).addAllItemIds(ids).build();
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

    var item = documentSnapshotApiFuture.get().toObject(Items.class);
    var expectedData = new Items(strUuid, itemName, Arrays.asList("testid", "testid2"));

    assertEquals(expectedData, item);
  }

  @Test
  void search0Test() throws InterruptedException, ExecutionException {
    // リクエスト作成
    var request = SearchRequest.newBuilder().build();
    StreamRecorder<SearchReply> responseObserver = StreamRecorder.create();

    // 呼び出し
    controller.search(request, responseObserver);

    // エラー関係
    assertNull(responseObserver.getError());

    // 結果確認
    var results = responseObserver.getValues();
    List<SearchReply> expected = new ArrayList<>();
    assertIterableEquals(expected, results);
  }

  @Test
  void searchTest() throws InterruptedException, ExecutionException {
    // リクエスト作成
    var request = SearchRequest.newBuilder().setName("虹").build();
    StreamRecorder<SearchReply> responseObserver = StreamRecorder.create();

    // mock データ作成
    var name1 = "ブルーオーブ";
    var name2 = "レッドオーブ";
    createDoc(uuid.toString(), "虹色のオーブ");
    createDoc(uuidSearch1.toString(), name1);
    createDoc(uuidSearch2.toString(), name2);

    // 呼び出し
    controller.search(request, responseObserver);

    // エラー関係
    assertNull(responseObserver.getError());

    // 結果確認
    var results = responseObserver.getValues();

    List<SearchReply> expected =
        Arrays.asList(SearchReply.newBuilder().setId(uuid.toString()).setName("虹色のオーブ")
            .addAllItemIds(Arrays.asList(
                Bean.newBuilder().setId(uuidSearch1.toString()).setName(name1).build(),
                Bean.newBuilder().setId(uuidSearch2.toString()).setName(name2).build()))
            .build());

    assertIterableEquals(expected, results);
  }

  /**
   * Doc作成
   *
   * @throws InterruptedException 例外
   * @throws ExecutionException 例外
   */
  void createDoc(String uuid, String name) throws InterruptedException, ExecutionException {

    var data = new Items(uuid, name, Arrays.asList(uuidSearch1.toString(), uuidSearch2.toString()));
    firestore.document("items/" + uuid).set(data).get();
  }

  @Test
  void updateTest() throws InterruptedException, ExecutionException {
    // リクエスト作成
    var name = "イエローオーブ";

    var request = UpdateRequest.newBuilder().setId(uuid.toString()).setName(name)
        .addAllItemIds(Arrays.asList(uuidSearch1.toString(), uuidSearch3.toString())).build();
    StreamRecorder<UpdateReply> responseObserver = StreamRecorder.create();

    // mock データ作成
    var name1 = "ブルーオーブ";
    var name2 = "レッドオーブ";
    createDoc(uuid.toString(), "虹色のオーブ");
    createDoc(uuidSearch1.toString(), name1);
    createDoc(uuidSearch2.toString(), name2);

    // 呼び出し
    controller.update(request, responseObserver);

    // エラー関係
    assertNull(responseObserver.getError());

    // 結果確認
    var results = responseObserver.getValues();
    var expected = Arrays.asList(UpdateReply.newBuilder().setStatus(Status.FINISH).build());
    assertEquals(expected, results);

    // 更新結果確認
    var strUuid = uuid.toString();
    ApiFuture<DocumentSnapshot> documentSnapshotApiFuture =
        firestore.document("items/" + strUuid).get();

    var item = documentSnapshotApiFuture.get().toObject(Items.class);
    var searchExpected = new Items(uuid.toString(), name,
        Arrays.asList(uuidSearch1.toString(), uuidSearch3.toString()));
    assertEquals(searchExpected, item);
  }

}
