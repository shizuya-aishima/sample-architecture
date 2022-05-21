package com.example.price;

import com.example.grpc.price.PriceGrpc.PriceImplBase;
import com.example.grpc.price.PriceOuterClass.CreateReply;
import com.example.grpc.price.PriceOuterClass.CreateRequest;
import com.example.grpc.price.PriceOuterClass.SearchReply;
import com.example.grpc.price.PriceOuterClass.SearchRequest;
import com.example.grpc.price.PriceOuterClass.UpdateReply;
import com.example.grpc.price.PriceOuterClass.UpdateRequest;
import org.lognet.springboot.grpc.GRpcService;
import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@GRpcService
@AllArgsConstructor
@Slf4j
public class Service extends PriceImplBase {
  @Override
  public void create(CreateRequest request, StreamObserver<CreateReply> responseObserver) {
    log.info("test");

    responseObserver.onCompleted();
  }

  @Override
  public void search(SearchRequest request, StreamObserver<SearchReply> responseObserver) {
    responseObserver.onCompleted();
  }


  @Override
  public void update(UpdateRequest request, StreamObserver<UpdateReply> responseObserver) {
    responseObserver.onCompleted();
  }
}
