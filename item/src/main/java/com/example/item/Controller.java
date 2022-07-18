package com.example.item;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.lognet.springboot.grpc.GRpcService;
import com.example.grpc.item.ItemGrpc.ItemImplBase;
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
import com.example.item.Bean.Expected;
import com.example.item.Bean.Items;
import com.example.item.Bean.Materials;
import com.example.item.price.PriceService;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@GRpcService
@AllArgsConstructor
@Slf4j
public class Controller extends ItemImplBase {
  Firestore firestore;
  PriceService priceService;

  @Override
  public void create(CreateRequest request, StreamObserver<CreateReply> responseObserver) {
    try {
      var uuid = UUID.randomUUID().toString();

      var duplication = findItemDoc(uuid);
      var unique = firestore.collection("items").whereEqualTo("name", request.getName()).get().get()
          .toObjects(Items.class);
      if (duplication != null || !unique.isEmpty()) {
        StatusRuntimeException exception =
            io.grpc.Status.INTERNAL.withDescription("id duplication").asRuntimeException();
        responseObserver.onError(exception);
        responseObserver.onCompleted();
        return;
      }

      // TODO: 処理
      var data = new Items(uuid, request.getName(), request.getItemIdsList().stream()
          .map((e) -> Materials.builder().id(e.getId()).quantity(e.getQuantity()).build()).toList(),
          Expected.builder().greatSuccess(request.getExpected().getGreatSuccess())
              .success(request.getExpected().getSuccess())
              .greatSuccessPrice(request.getExpected().getGreatSuccessPrice())
              .successPrice(request.getExpected().getSuccessPrice()).build());

      // .get() blocks on response
      WriteResult writeResult = firestore.document("items/" + uuid).set(data).get();
      log.info("{}", writeResult);

      responseObserver.onNext(CreateReply.newBuilder().setStatus(Status.PENDING).build());

      // TODO: トランザクションがいるような処理
      // 単価情報があった場合、単価情報を書き込むなど
      var price = com.example.grpc.price.PriceOuterClass.CreateRequest.newBuilder().setId(uuid)
          .setPrice(request.getPrice()).build();
      var replies = priceService.blockingStub().create(price);

      while (replies.hasNext()) {
        log.info("status: {}", replies.next().getStatus());
      }
    } catch (InterruptedException | ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      log.error("{}", e);

      StatusRuntimeException exception =
          io.grpc.Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException();
      responseObserver.onError(exception);
      responseObserver.onCompleted();
      return;
    }
    responseObserver.onNext(CreateReply.newBuilder().setStatus(Status.FINISH).build());
    responseObserver.onCompleted();
  }

  @Override
  public void search(SearchRequest request, StreamObserver<SearchReply> responseObserver) {
    try {
      var key = request.getName();
      var data = firestore.collection("items").orderBy("name").startAt(key).endAt(key + "\uf8ff")
          .get().get().toObjects(Items.class);

      if (!data.isEmpty()) {
        log.info("data: {}", data.toString());
      }

      data.stream().map((e) -> SearchReply.newBuilder().setId(e.getId()).setName(e.getName())
          .addAllItemIds(
              e.getItemIds().stream().map((id) -> searchId(id.getId(), id.getQuantity())).toList())
          .setExpected(ExpectedValue.newBuilder().setGreatSuccess(e.getExpected().getGreatSuccess())
              .setGreatSuccessPrice(e.getExpected().getGreatSuccessPrice())
              .setSuccess(e.getExpected().getSuccess())
              .setSuccessPrice(e.getExpected().getSuccessPrice()))
          .build()).sorted((a, b) -> a.getName().compareTo(b.getName()))
          .forEach((e) -> responseObserver.onNext(e));
      // var data2 = SearchReply.newBuilder().setId()
    } catch (InterruptedException | ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      log.error("{}", e);

      StatusRuntimeException exception =
          io.grpc.Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException();
      responseObserver.onError(exception);
      responseObserver.onCompleted();
      return;
    }
    responseObserver.onCompleted();
  }

  /**
   * items を コレクション で検索します。
   *
   * @param id コレクション
   * @return 品目データ
   */
  private Bean searchId(String id, long quantity) {
    try {
      log.info("id : {}", id);
      var data = findItemDoc(id);
      var priceData =
          priceService.blockingStub().search(com.example.grpc.price.PriceOuterClass.SearchRequest
              .newBuilder().setId(data.getId()).build());
      return Bean.newBuilder().setId(data.getId()).setName(data.getName()).setQuantity(quantity)
          .setPrice(priceData.getPrice()).build();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      log.error("{}", e);
      return Bean.newBuilder().build();
    }
  }

  @Override
  public void update(UpdateRequest request, StreamObserver<UpdateReply> responseObserver) {
    var uuid = request.getId();
    var data = new Items(uuid, request.getName(), request.getItemIdsList().stream()
        .map((e) -> Materials.builder().id(e.getId()).quantity(e.getQuantity()).build()).toList(),
        Expected.builder().greatSuccess(request.getExpected().getGreatSuccess())
            .success(request.getExpected().getSuccess())
            .greatSuccessPrice(request.getExpected().getGreatSuccessPrice())
            .successPrice(request.getExpected().getSuccessPrice()).build());

    // .get() blocks on response
    try {
      WriteResult writeResult = firestore.document("items/" + uuid).set(data).get();
      log.info("{}", writeResult);
      var priceData =
          priceService.blockingStub().update(com.example.grpc.price.PriceOuterClass.UpdateRequest
              .newBuilder().setId(uuid).setPrice(request.getPrice()).build());
      log.info("{}", priceData.getStatus());
    } catch (InterruptedException | ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      log.error("{}", e);

      StatusRuntimeException exception =
          io.grpc.Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException();
      responseObserver.onError(exception);
      responseObserver.onCompleted();
      return;
    }
    responseObserver.onNext(UpdateReply.newBuilder().setStatus(Status.FINISH).build());
    responseObserver.onCompleted();
  }

  @Override
  public void find(ItemFindRequest request, StreamObserver<ItemFindReply> responseObserver) {
    try {
      var item = findItemDoc(request.getId());
      responseObserver.onNext(ItemFindReply.newBuilder().setId(item.getId()).setName(item.getName())
          .addAllItemIds(
              item.getItemIds().stream().map((e) -> searchId(e.getId(), e.getQuantity())).toList())
          .setExpected(
              ExpectedValue.newBuilder().setGreatSuccess(item.getExpected().getGreatSuccess())
                  .setGreatSuccessPrice(item.getExpected().getGreatSuccessPrice())
                  .setSuccess(item.getExpected().getSuccess())
                  .setSuccessPrice(item.getExpected().getSuccessPrice()).build())
          .build());
    } catch (InterruptedException | ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      log.error("{}", e);

      StatusRuntimeException exception =
          io.grpc.Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException();
      responseObserver.onError(exception);
      responseObserver.onCompleted();
      return;
    }
    responseObserver.onCompleted();
  }

  /**
   * Docを取得します。
   *
   * @param id ID
   * @return Items
   * @throws InterruptedException
   * @throws ExecutionException
   */
  private Items findItemDoc(String id) throws InterruptedException, ExecutionException {
    return firestore.document("items/" + id).get().get().toObject(Items.class);
  }

  private Materials findMaterialDoc(String id) throws InterruptedException, ExecutionException {
    return firestore.document("items/" + id).get().get().toObject(Materials.class);
  }
}
