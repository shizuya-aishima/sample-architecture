package com.example.item.Bean;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Items {

  public enum Status {
    PENDING, FINISH
  }

  private String id;
  private String name;
  private List<String> itemIds;
}
