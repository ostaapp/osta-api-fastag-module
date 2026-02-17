package com.dipcoin.api.fraudMgmt;

import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class EventStatus {
  private int count;
  private List<String> Ipaddress;
  private List<String> requestTime;
  private Map<String, String> ostaProcessed;

}
