package com.example.item.Bean;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Items {

  public enum Status {
    PENDING, FINISH
  }

  private String id;
  private String name;
  private List<Items> itemIds;
}
