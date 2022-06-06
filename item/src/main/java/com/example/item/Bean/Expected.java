package com.example.item.Bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expected {
  /** 大成功個数 */
  private long greatSuccess;
  /** 成功個数 */
  private long success;
  /** 大成功単価 */
  private long greatSuccessPrice;
  /** 大成功単価 */
  private long successPrice;
}
