package com.example.item;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import com.example.grpc.item.ItemGrpc.ItemImplBase;
import com.example.grpc.item.ItemOuterClass.CreateReply;
import com.example.grpc.item.ItemOuterClass.CreateRequest;
import com.example.grpc.item.ItemOuterClass.SearchReply;
import com.example.grpc.item.ItemOuterClass.SearchRequest;
import com.example.grpc.item.ItemOuterClass.Status;
import com.example.grpc.item.ItemOuterClass.UpdateReply;
import com.example.grpc.item.ItemOuterClass.UpdateRequest;
import com.example.item.Bean.Items;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import org.lognet.springboot.grpc.GRpcService;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@GRpcService
@AllArgsConstructor
@Slf4j
public class Controller extends ItemImplBase {
  Firestore firestore;

  @Override
  public void create(CreateRequest request, StreamObserver<CreateReply> responseObserver) {
    try {
      var uuid = UUID.randomUUID().toString();
      // TODO: 処理
      var data = new Items(uuid, request.getName(), Arrays.asList("testid"));

      // .get() blocks on response
      WriteResult writeResult = firestore.document("items/" + uuid).set(data).get();
      log.info("{}", writeResult);

      var reply = CreateReply.newBuilder().setStatus(Status.PENDING).build();
      responseObserver.onNext(reply);

      // TODO: トランザクションがいるような処理
      // 単価情報があった場合、単価情報を書き込むなど

      var reply2 = CreateReply.newBuilder().setStatus(Status.FINISH).build();
      responseObserver.onNext(reply2);
      responseObserver.onCompleted();
    } catch (InterruptedException | ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      log.error("{}", e);
      var errorReply = CreateReply.newBuilder().setStatus(Status.FAIL).build();
      responseObserver.onNext(errorReply);
      responseObserver.onCompleted();
    }
  }

  @Override
  public void search(SearchRequest request, StreamObserver<SearchReply> responseObserver) {
    // TODO Auto-generated method stub
    super.search(request, responseObserver);
  }

  @Override
  public void update(UpdateRequest request, StreamObserver<UpdateReply> responseObserver) {
    // TODO Auto-generated method stub
    super.update(request, responseObserver);
  }
}
