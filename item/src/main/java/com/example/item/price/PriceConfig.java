package com.example.item.price;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Component
@ConfigurationProperties(prefix = "grpc.price")
@Data
public class PriceConfig {
  private String ip;
  private int port;
}
