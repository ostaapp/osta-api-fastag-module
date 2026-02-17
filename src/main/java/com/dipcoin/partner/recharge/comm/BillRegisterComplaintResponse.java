package com.dipcoin.partner.recharge.comm;

public class BillRegisterComplaintResponse extends BillResponse {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public static interface Fields extends BillResponse.Fields {

    public static final String COMPLAINT_ID = "ComplaintId";
    public static final String COMPLAINT_STATUS = "ComplaintStatus";
    public static final String OPEN_COMPLAINT = "OpenComplaint";
    public static final String BILL_COMPLAINT_REGISTRATION_DATE = "TxnTs";
    public static final String ASSIGNED_TO = "AssignedTo";
    public static final String REMARKS = "remarks"; // epay 3.0
  }

  public final Object getComplaintId() {
    return this.get(Fields.COMPLAINT_ID);
  }

  public final void setComplaintId(String complaintId) {
    this.put(Fields.COMPLAINT_ID, complaintId);
  }

  public final Object getComplaintStatus() {
    return this.get(Fields.COMPLAINT_STATUS);
  }

  public final void setComplaintStatus(String complaintStatus) {
    this.put(Fields.COMPLAINT_STATUS, complaintStatus);
  }

  public final Object getOpenComplaint() {
    return this.get(Fields.OPEN_COMPLAINT);
  }

  public final void setOpenComplaint(String openComplaint) {
    this.put(Fields.OPEN_COMPLAINT, openComplaint);
  }

  public final Object getAssignedTo() {
    return this.get(Fields.ASSIGNED_TO);
  }

  public final void setAssignedTo(String assignedTo) {
    this.put(Fields.ASSIGNED_TO, assignedTo);
  }

  public final Object getBillComplaintRegistrationDate() {
    return this.get(Fields.BILL_COMPLAINT_REGISTRATION_DATE);
  }

  public final void setBillComplaintRegistrationDate(String billComplaintRegistrationDate) {
    this.put(Fields.BILL_COMPLAINT_REGISTRATION_DATE, billComplaintRegistrationDate);
  }

  public final String getRemarks() {
	return (String) this.get(Fields.REMARKS);
  }

  public final void setRemarks(String remarks) {
	this.put(Fields.REMARKS, remarks);
  }
}
