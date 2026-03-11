package com.dipcoin.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@JsonInclude(Include.NON_NULL)
@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class Pagination {
  private Long startRange;
  private Long endRange;
  private Integer start;
  private Boolean scanCompleted;
  private Integer total;

}
