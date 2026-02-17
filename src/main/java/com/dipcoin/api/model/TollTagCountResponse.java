package com.dipcoin.api.model;

public class TollTagCountResponse {
  
  private Long tagApprovalPendingCounts;
  private Long tagRejectedCounts;
  private Long dispatchedTagCounts;
  private Long deliveryTagCounts;


  public Long getDispatchedTagCounts() {
    return dispatchedTagCounts;
  }

  public void setDispatchedTagCounts(Long dispatchedTagCounts) {
    this.dispatchedTagCounts = dispatchedTagCounts;
  }

  public Long getDeliveryTagCounts() {
    return deliveryTagCounts;
  }

  public void setDeliveryTagCounts(Long deliveryTagCounts) {
    this.deliveryTagCounts = deliveryTagCounts;
  }

  public Long getTagApprovalPendingCounts() {
    return tagApprovalPendingCounts;
  }

  public void setTagApprovalPendingCounts(Long tagApprovalPendingCounts) {
    this.tagApprovalPendingCounts = tagApprovalPendingCounts;
  }

  public Long getTagRejectedCounts() {
    return tagRejectedCounts;
  }

  public void setTagRejectedCounts(Long tagRejectedCounts) {
    this.tagRejectedCounts = tagRejectedCounts;
  }

  
 }
