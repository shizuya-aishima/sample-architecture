package com.example.item;

import com.example.grpc.item.ItemGrpc.ItemImplBase;
import com.example.grpc.item.ItemOuterClass.CreateReply;
import com.example.grpc.item.ItemOuterClass.CreateRequest;

import org.lognet.springboot.grpc.GRpcService;

import io.grpc.stub.StreamObserver;

@GRpcService
public class Controller extends ItemImplBase {
  @Override
  public void create(CreateRequest request, StreamObserver<CreateReply> responseObserver) {
    // TODO Auto-generated method stub
    super.create(request, responseObserver);

  }
}
