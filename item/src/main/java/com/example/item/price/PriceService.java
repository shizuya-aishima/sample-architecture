package com.example.item.price;

import org.springframework.stereotype.Service;
import com.example.grpc.price.PriceGrpc;
import com.example.grpc.price.PriceGrpc.PriceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PriceService {
  private PriceConfig config;

  /**
   * price GRPC 作成
   *
   * @return スタブ
   */
  public PriceBlockingStub blockingStub() {
    ManagedChannel channel = ManagedChannelBuilder.forAddress(config.getIp(), config.getPort())
        .useTransportSecurity().build();
    return PriceGrpc.newBlockingStub(channel);
  }
}
