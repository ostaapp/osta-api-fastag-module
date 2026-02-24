package com.dipcoin.api.fraudMgmt;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class Metadata {

  private String role;

  private String updatedAt;

}
