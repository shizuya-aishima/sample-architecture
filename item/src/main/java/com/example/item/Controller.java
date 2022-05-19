package com.example.item;

import com.example.grpc.item.ItemGrpc.ItemImplBase;
import com.example.grpc.item.ItemOuterClass.CreateReply;
import com.example.grpc.item.ItemOuterClass.CreateRequest;
import com.example.grpc.item.ItemOuterClass.SearchReply;
import com.example.grpc.item.ItemOuterClass.SearchRequest;
import com.example.grpc.item.ItemOuterClass.Status;
import com.example.grpc.item.ItemOuterClass.UpdateReply;
import com.example.grpc.item.ItemOuterClass.UpdateRequest;
import com.google.cloud.firestore.Firestore;
import org.lognet.springboot.grpc.GRpcService;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;

@GRpcService
@AllArgsConstructor
public class Controller extends ItemImplBase {
  Firestore firestore;

  @Override
  public void create(CreateRequest request, StreamObserver<CreateReply> responseObserver) {
    // TODO: 処理
    var reply = CreateReply.newBuilder().setStatus(Status.PENDING).build();
    responseObserver.onNext(reply);
    var reply2 = CreateReply.newBuilder().setStatus(Status.FINISH).build();
    responseObserver.onNext(reply2);
    responseObserver.onCompleted();
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
