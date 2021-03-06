package com.example.item;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import com.example.grpc.item.ItemOuterClass.Bean;
import com.example.grpc.item.ItemOuterClass.CreateReply;
import com.example.grpc.item.ItemOuterClass.CreateRequest;
import com.example.grpc.item.ItemOuterClass.ExpectedValue;
import com.example.grpc.item.ItemOuterClass.ItemFindReply;
import com.example.grpc.item.ItemOuterClass.ItemFindRequest;
import com.example.grpc.item.ItemOuterClass.SearchReply;
import com.example.grpc.item.ItemOuterClass.SearchRequest;
import com.example.grpc.item.ItemOuterClass.Status;
import com.example.grpc.item.ItemOuterClass.UpdateReply;
import com.example.grpc.item.ItemOuterClass.UpdateRequest;
import com.example.grpc.price.PriceGrpc.PriceBlockingStub;
import com.example.item.Bean.Expected;
import com.example.item.Bean.Items;
import com.example.item.Bean.Materials;
import com.example.item.price.PriceService;
import com.google.api.core.ApiFuture;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import io.grpc.internal.testing.StreamRecorder;
import io.grpc.testing.GrpcServerRule;

// @SpringBootTest
// @ExtendWith(SpringExtension.class)
public class ControllerTests {

  private Controller controller;

  private Firestore firestore;

  private PriceBlockingStub priceBlockingStub = Mockito.mock(PriceBlockingStub.class);

  @Rule
  public GrpcServerRule grpcServerRule = new GrpcServerRule().directExecutor();

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

  @BeforeEach
  public void setup() throws IOException {
    // mock ??? firestore ??????
    firestore = FirestoreOptions.getDefaultInstance().toBuilder().setProjectId("test")
        .setHost("localhost:9000").setCredentials(new FirestoreOptions.EmulatorCredentials())
        .setCredentialsProvider(
            FixedCredentialsProvider.create(new FirestoreOptions.EmulatorCredentials()))
        .build().getService();
    var priceService = Mockito.mock(PriceService.class);
    Mockito.when(priceService.blockingStub()).thenReturn(priceBlockingStub);
    controller = new Controller(firestore, priceService);
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

  /**
   * ???????????????????????????
   *
   * @throws Exception ?????????
   */
  @Test
  void createTest() throws Exception {

    // ??????????????????
    var itemName = "itemName";
    var price = 10000;

    CreateRequest request = CreateRequest.newBuilder().setName(itemName)
        .addAllItemIds(Arrays.asList(Bean.newBuilder().setId(uuidSearch1.toString())
            .setName("name2").setQuantity(3).build()))
        .setPrice(price)
        .setExpected(ExpectedValue.newBuilder().setGreatSuccess(10).setSuccess(2).build()).build();
    StreamRecorder<CreateReply> responseObserver = StreamRecorder.create();

    // mock ?????????
    var mockRequest = com.example.grpc.price.PriceOuterClass.CreateRequest.newBuilder()
        .setId(uuid.toString()).setPrice(price).build();
    var mockReply = Arrays.asList(
        com.example.grpc.price.PriceOuterClass.CreateReply.newBuilder()
            .setStatus(com.example.grpc.price.PriceOuterClass.Status.PENDING).build(),
        com.example.grpc.price.PriceOuterClass.CreateReply.newBuilder()
            .setStatus(com.example.grpc.price.PriceOuterClass.Status.FINISH).build());
    Mockito.when(priceBlockingStub.create(mockRequest)).thenReturn(mockReply.iterator());

    // ????????????
    controller.create(request, responseObserver);
    if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
      fail("The call did not terminate in time");
    }

    // ???????????????
    assertNull(responseObserver.getError());

    // ???????????????
    var results = responseObserver.getValues();
    List<CreateReply> expected =
        Arrays.asList(CreateReply.newBuilder().setStatus(Status.PENDING).build(),
            CreateReply.newBuilder().setStatus(Status.FINISH).build());
    assertIterableEquals(expected, results);

    // firestore ????????????
    var strUuid = uuid.toString();
    ApiFuture<DocumentSnapshot> documentSnapshotApiFuture =
        firestore.document("items/" + strUuid).get();
    // firestore ???????????????

    var item = documentSnapshotApiFuture.get().toObject(Items.class);
    var expectedData = Items.builder().id(strUuid).name(itemName)
        .itemIds(Arrays.asList(Materials.builder().id(uuidSearch1.toString()).quantity(3).build()))
        .expected(Expected.builder().greatSuccess(10).success(2).build()).build();

    assertEquals(expectedData, item);
  }


  @Test
  void createError() throws Exception {

    // ??????????????????
    var itemName = "itemName";
    var ids = Arrays.asList("testid", "testid2");
    var price = 10000;
    CreateRequest request = CreateRequest.newBuilder().setName(itemName)
        .addAllItemIds(
            Arrays.asList(Bean.newBuilder().setId(uuidSearch1.toString()).setName("name2").build()))
        .setPrice(price).build();
    StreamRecorder<CreateReply> responseObserver = StreamRecorder.create();

    // ???????????????Doc??????
    createDoc(uuid.toString(), "??????????????????");

    // ????????????
    controller.create(request, responseObserver);
    if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
      fail("The call did not terminate in time");
    }

    // ???????????????
    assertNotNull(responseObserver.getError());
  }

  @Test
  void createErrorSameName() throws Exception {

    // ??????????????????
    var itemName = "??????????????????";
    var ids = Arrays.asList("testid", "testid2");
    var price = 10000;
    CreateRequest request = CreateRequest.newBuilder().setName(itemName)
        .addAllItemIds(
            Arrays.asList(Bean.newBuilder().setId(uuidSearch1.toString()).setName("name2").build()))
        .setPrice(price).build();
    StreamRecorder<CreateReply> responseObserver = StreamRecorder.create();

    // ???????????????Doc??????
    createDoc(uuidSearch1.toString(), "??????????????????");

    // ????????????
    controller.create(request, responseObserver);
    if (!responseObserver.awaitCompletion(5, TimeUnit.SECONDS)) {
      fail("The call did not terminate in time");
    }

    // ???????????????
    assertNotNull(responseObserver.getError());
  }

  @Test
  void search0Test() throws InterruptedException, ExecutionException {
    // ?????????????????????
    var request = SearchRequest.newBuilder().build();
    StreamRecorder<SearchReply> responseObserver = StreamRecorder.create();

    // ????????????
    controller.search(request, responseObserver);

    // ???????????????
    assertNull(responseObserver.getError());

    // ????????????
    var results = responseObserver.getValues();
    List<SearchReply> expected = new ArrayList<>();
    assertIterableEquals(expected, results);
  }

  @Test
  void searchTest() throws InterruptedException, ExecutionException {
    // ?????????????????????
    var request = SearchRequest.newBuilder().setName("???").build();
    StreamRecorder<SearchReply> responseObserver = StreamRecorder.create();

    // mock ???????????????
    var name1 = "??????????????????";
    var name2 = "??????????????????";
    createDoc(uuid.toString(), "??????????????????");
    createDoc(uuidSearch1.toString(), name1);
    createDoc(uuidSearch2.toString(), name2);

    // price ??? mock ???
    var priceRequest1 = com.example.grpc.price.PriceOuterClass.SearchRequest.newBuilder()
        .setId(uuidSearch1.toString()).build();
    Mockito.when(priceBlockingStub.search(priceRequest1)).thenReturn(
        com.example.grpc.price.PriceOuterClass.SearchReply.newBuilder().setPrice(1000).build());
    var priceRequest2 = com.example.grpc.price.PriceOuterClass.SearchRequest.newBuilder()
        .setId(uuidSearch2.toString()).build();
    Mockito.when(priceBlockingStub.search(priceRequest2)).thenReturn(
        com.example.grpc.price.PriceOuterClass.SearchReply.newBuilder().setPrice(2000).build());

    // ????????????
    controller.search(request, responseObserver);

    // ???????????????
    assertNull(responseObserver.getError());

    // ????????????
    var results = responseObserver.getValues();

    List<SearchReply> expected = Arrays.asList(SearchReply.newBuilder().setId(uuid.toString())
        .setName("??????????????????")
        .addAllItemIds(Arrays.asList(
            Bean.newBuilder().setId(uuidSearch1.toString()).setName(name1).setQuantity(3)
                .setPrice(1000).build(),
            Bean.newBuilder().setId(uuidSearch2.toString()).setName(name2).setQuantity(3)
                .setPrice(2000).build()))
        .setExpected(ExpectedValue.newBuilder().setGreatSuccess(10).setSuccess(2).build()).build());

    assertIterableEquals(expected, results);
  }

  /**
   * Doc??????
   *
   * @throws InterruptedException ??????
   * @throws ExecutionException ??????
   */
  void createDoc(String uuid, String name) throws InterruptedException, ExecutionException {
    var data = Items.builder().id(uuid).name(name)
        .itemIds(Arrays.asList(Materials.builder().id(uuidSearch1.toString()).quantity(3).build(),
            Materials.builder().id(uuidSearch2.toString()).quantity(3).build()))
        .expected(Expected.builder().success(2).greatSuccess(10).build()).build();
    firestore.document("items/" + uuid).set(data).get();
  }

  @Test
  void updateTest() throws InterruptedException, ExecutionException {
    // ?????????????????????
    var name = "?????????????????????";

    var request =
        UpdateRequest.newBuilder().setId(uuid.toString()).setName(name).setPrice(1000)
            .addAllItemIds(Arrays.asList(
                Bean.newBuilder().setId(uuidSearch1.toString()).setQuantity(5).build(),
                Bean.newBuilder().setId(uuidSearch2.toString()).setQuantity(6).build()))
            .setExpected(ExpectedValue.newBuilder().setGreatSuccess(10).setSuccess(2).build())
            .build();
    StreamRecorder<UpdateReply> responseObserver = StreamRecorder.create();

    // mock ???????????????
    var name1 = "??????????????????";
    var name2 = "??????????????????";
    createDoc(uuid.toString(), "??????????????????");
    createDoc(uuidSearch1.toString(), name1);
    createDoc(uuidSearch2.toString(), name2);

    // mock ?????????
    var mockRequest = com.example.grpc.price.PriceOuterClass.UpdateRequest.newBuilder()
        .setId(uuid.toString()).setPrice(1000).build();
    var mockReply = com.example.grpc.price.PriceOuterClass.UpdateReply.newBuilder()
        .setStatus(com.example.grpc.price.PriceOuterClass.Status.FINISH).build();
    Mockito.when(priceBlockingStub.update(mockRequest)).thenReturn(mockReply);

    // ????????????
    controller.update(request, responseObserver);

    // ???????????????
    assertNull(responseObserver.getError());

    // ????????????
    var results = responseObserver.getValues();
    var expected = Arrays.asList(UpdateReply.newBuilder().setStatus(Status.FINISH).build());
    assertEquals(expected, results);

    // ??????????????????
    var strUuid = uuid.toString();
    ApiFuture<DocumentSnapshot> documentSnapshotApiFuture =
        firestore.document("items/" + strUuid).get();

    var item = documentSnapshotApiFuture.get().toObject(Items.class);
    var searchExpected = new Items(uuid.toString(), name,
        Arrays.asList(Materials.builder().id(uuidSearch1.toString()).quantity(5).build(),
            Materials.builder().id(uuidSearch2.toString()).quantity(6).build()),
        Expected.builder().greatSuccess(10).success(2).build());
    assertEquals(searchExpected, item);
    verify(priceBlockingStub, times(1)).update(mockRequest);
  }

  @Test
  void findTest() throws InterruptedException, ExecutionException {
    // ?????????????????????
    var request = ItemFindRequest.newBuilder().setId(uuid.toString()).build();
    StreamRecorder<ItemFindReply> responseObserver = StreamRecorder.create();

    // mock ???????????????
    var name1 = "??????????????????";
    var name2 = "??????????????????";
    createDoc(uuid.toString(), "??????????????????");
    createDoc(uuidSearch1.toString(), name1);
    createDoc(uuidSearch2.toString(), name2);

    // price ??? mock ???
    var priceRequest0 = com.example.grpc.price.PriceOuterClass.SearchRequest.newBuilder()
        .setId(uuid.toString()).build();
    Mockito.when(priceBlockingStub.search(priceRequest0)).thenReturn(
        com.example.grpc.price.PriceOuterClass.SearchReply.newBuilder().setPrice(3000).build());

    // price ??? mock ???
    var priceRequest1 = com.example.grpc.price.PriceOuterClass.SearchRequest.newBuilder()
        .setId(uuidSearch1.toString()).build();
    Mockito.when(priceBlockingStub.search(priceRequest1)).thenReturn(
        com.example.grpc.price.PriceOuterClass.SearchReply.newBuilder().setPrice(1000).build());
    var priceRequest2 = com.example.grpc.price.PriceOuterClass.SearchRequest.newBuilder()
        .setId(uuidSearch2.toString()).build();
    Mockito.when(priceBlockingStub.search(priceRequest2)).thenReturn(
        com.example.grpc.price.PriceOuterClass.SearchReply.newBuilder().setPrice(2000).build());

    // ????????????
    controller.find(request, responseObserver);

    // ???????????????
    assertNull(responseObserver.getError());

    // ????????????
    var results = responseObserver.getValues();

    List<ItemFindReply> expected = Arrays
        .asList(ItemFindReply.newBuilder().setId(uuid.toString()).setName("??????????????????").setPrice(3000)
            .addAllItemIds(Arrays.asList(
                Bean.newBuilder().setId(uuidSearch1.toString()).setQuantity(3).setName(name1)
                    .setPrice(1000).build(),
                Bean.newBuilder().setId(uuidSearch2.toString()).setQuantity(3).setName(name2)
                    .setPrice(2000).build()))
            .setExpected(ExpectedValue.newBuilder().setGreatSuccess(10).setSuccess(2).build())
            .build());

    assertIterableEquals(expected, results);

  }
}
