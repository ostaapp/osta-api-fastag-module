package com.dipcoin.partner.recharge.comm;

import java.util.List;

public class MDMBillerFetchResponse {

  private List<MDMBillers> biller;

  public List<MDMBillers> getBiller() {
    return biller;
  }

  public void setBiller(List<MDMBillers> biller) {
    this.biller = biller;
  }


}
