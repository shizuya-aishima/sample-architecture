package com.example.price;

import java.util.concurrent.ExecutionException;
import com.example.grpc.price.PriceGrpc.PriceImplBase;
import com.example.grpc.price.PriceOuterClass.CreateReply;
import com.example.grpc.price.PriceOuterClass.CreateRequest;
import com.example.grpc.price.PriceOuterClass.SearchReply;
import com.example.grpc.price.PriceOuterClass.SearchRequest;
import com.example.grpc.price.PriceOuterClass.Status;
import com.example.grpc.price.PriceOuterClass.UpdateReply;
import com.example.grpc.price.PriceOuterClass.UpdateRequest;
import com.example.price.Bean.Prices;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import org.lognet.springboot.grpc.GRpcService;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@GRpcService
@AllArgsConstructor
@Slf4j
public class Service extends PriceImplBase {

  private Firestore firestore;

  @Override
  public void create(CreateRequest request, StreamObserver<CreateReply> responseObserver) {

    // 登録データ作成
    var id = request.getId();
    var price = new Prices(request.getId(), request.getPrice());

    // 更新処理
    try {
      WriteResult writeResult = firestore.document("prices/" + id).set(price).get();
      log.info("{}", writeResult);
    } catch (InterruptedException | ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      log.error("{}", e);

      StatusRuntimeException exception =
          io.grpc.Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException();
      responseObserver.onError(exception);
    }

    // 更新済みを返す
    var pending = CreateReply.newBuilder().setStatus(Status.PENDING).build();
    responseObserver.onNext(pending);

    // 完了を返す
    var finish = CreateReply.newBuilder().setStatus(Status.FINISH).build();
    responseObserver.onNext(finish);
    responseObserver.onCompleted();
  }

  @Override
  public void search(SearchRequest request, StreamObserver<SearchReply> responseObserver) {
    try {
      // 検索実行
      var data = firestore.document("prices/" + request.getId()).get().get().toObject(Prices.class);
      responseObserver.onNext(SearchReply.newBuilder().setPrice(data.getPrice()).build());
    } catch (InterruptedException | ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      log.error("{}", e);

      StatusRuntimeException exception =
          io.grpc.Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException();
      responseObserver.onError(exception);
    }
    responseObserver.onCompleted();
  }


  @Override
  public void update(UpdateRequest request, StreamObserver<UpdateReply> responseObserver) {
    var data = new Prices(request.getId(), request.getPrice());
    try {
      WriteResult writeResult = firestore.document("prices/" + request.getId()).set(data).get();
      log.info("{}", writeResult);
    } catch (InterruptedException | ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      log.error("{}", e);

      StatusRuntimeException exception =
          io.grpc.Status.INTERNAL.withDescription(e.getMessage()).withCause(e).asRuntimeException();
      responseObserver.onError(exception);
    }
    responseObserver.onNext(UpdateReply.newBuilder().setStatus(Status.FINISH).build());
    responseObserver.onCompleted();
  }
}
