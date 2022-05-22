package com.example.price;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import com.example.grpc.price.PriceOuterClass.CreateReply;
import com.example.grpc.price.PriceOuterClass.CreateRequest;
import com.example.grpc.price.PriceOuterClass.SearchReply;
import com.example.grpc.price.PriceOuterClass.SearchRequest;
import com.example.grpc.price.PriceOuterClass.Status;
import com.example.grpc.price.PriceOuterClass.UpdateReply;
import com.example.grpc.price.PriceOuterClass.UpdateRequest;
import com.example.price.Bean.Prices;
import com.google.api.core.ApiFuture;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.firestore.CollectionReference;
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

public class ServiceTests {

  private Service testService;
  private Firestore firestore;
  private static final UUID uuid = UUID.fromString("5af48f3b-468b-4ae0-a065-7d7ac70b37a8");

  @BeforeAll
  static public void init() {
    // initialize the mock of the static method
    MockedStatic<UUID> mock = Mockito.mockStatic(UUID.class);
    // define the behaviour of the mock
    mock.when(UUID::randomUUID).thenReturn(uuid);
  }

  @BeforeEach
  public void setup() {
    // mock の firestore 作成
    firestore = FirestoreOptions.getDefaultInstance().toBuilder().setProjectId("test")
        .setHost("localhost:9000").setCredentials(new FirestoreOptions.EmulatorCredentials())
        .setCredentialsProvider(
            FixedCredentialsProvider.create(new FirestoreOptions.EmulatorCredentials()))
        .build().getService();
    testService = new Service(firestore);
  }

  @AfterEach
  public void closing() {
    deleteCollection(firestore.collection("items"), 100);
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


  @Test
  public void creatTest() throws InterruptedException, ExecutionException {
    // 引数作成
    CreateRequest request =
        CreateRequest.newBuilder().setId(uuid.toString()).setPrice(1000).build();
    StreamRecorder<CreateReply> responseObserver = StreamRecorder.create();

    // 実行
    testService.create(request, responseObserver);

    // エラーチェック
    assertNull(responseObserver.getError());

    // 結果確認
    var results = responseObserver.getValues();
    List<CreateReply> expected =
        Arrays.asList(CreateReply.newBuilder().setStatus(Status.PENDING).build(),
            CreateReply.newBuilder().setStatus(Status.FINISH).build());
    assertIterableEquals(expected, results);

    var price = firestore.document("prices/" + uuid.toString()).get().get().toObject(Prices.class);
    Prices firebaseExpected = new Prices(uuid.toString(), 1000);
    assertEquals(firebaseExpected, price);
  }

  @Test
  public void searchTest() throws InterruptedException, ExecutionException {

    // firestore にデータ作成
    createDoc(uuid.toString(), 200);
    // 引数作成
    SearchRequest request = SearchRequest.newBuilder().setId(uuid.toString()).build();
    StreamRecorder<SearchReply> responseObserver = StreamRecorder.create();

    // 実行
    testService.search(request, responseObserver);

    // エラーチェック
    assertNull(responseObserver.getError());

    // 結果確認
    var results = responseObserver.getValues();
    List<SearchReply> expected = Arrays.asList(SearchReply.newBuilder().setPrice(200).build());
    assertIterableEquals(expected, results);
  }

  @Test
  public void updateTest() throws InterruptedException, ExecutionException {

    // テストデータ作成
    createDoc(uuid.toString(), 300);

    // 引数作成
    UpdateRequest request =
        UpdateRequest.newBuilder().setId(uuid.toString()).setPrice(5000).build();
    StreamRecorder<UpdateReply> responseObserver = StreamRecorder.create();

    // 実行
    testService.update(request, responseObserver);

    // エラーチェック
    assertNull(responseObserver.getError());

    // 結果確認
    var results = responseObserver.getValues();
    List<UpdateReply> expected =
        Arrays.asList(UpdateReply.newBuilder().setStatus(Status.FINISH).build());
    assertIterableEquals(expected, results);

    var actual = searchDoc(uuid.toString());
    var expectedData = new Prices(uuid.toString(), 5000);
    assertEquals(expectedData, actual);
  }

  /**
   * prices を作成します。
   *
   * @param id ID
   * @param price 単価
   * @throws ExecutionException エラー
   * @throws InterruptedException エラー
   */
  private void createDoc(String id, long price) throws InterruptedException, ExecutionException {
    var priceData = new Prices(uuid.toString(), price);
    firestore.document("prices/" + uuid.toString()).set(priceData).get();
  }

  /**
   * prices を検索します。
   *
   * @param id id
   * @return price
   * @throws InterruptedException
   * @throws ExecutionException
   */
  private Prices searchDoc(String id) throws InterruptedException, ExecutionException {
    return firestore.document("prices/" + id).get().get().toObject(Prices.class);
  }
}
