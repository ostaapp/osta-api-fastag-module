package com.dipcoin.partner.recharge.comm;

import java.util.HashMap;
import java.util.List;

public class BillInfoResponse extends BillResponse {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private FieldMapper fieldMapper;
  private List<FieldMapper> fieldMapperList;


  public static interface Fields extends BillResponse.Fields {
    public static final String BILL_DETAIL = "BillDetail";
    public static final String EURONET_REFERENCE_NUMBER = "EuronetRefNo";
  }

  public final HashMap<String, Object> getBillDetail() {
    return (HashMap<String, Object>) this.get(Fields.BILL_DETAIL);
  }

  public final void setBillDetail(BillDetail billDetail) {
    this.put(Fields.BILL_DETAIL, billDetail);
  }

  public final Long getEuronetRefNo() {
	  Object value = this.get(Fields.EURONET_REFERENCE_NUMBER);
	    if (value instanceof Integer) {
	        return ((Integer) value).longValue();
	    }
    return (Long) this.get(Fields.EURONET_REFERENCE_NUMBER);
  }
 

  public final void setEuronetRefNo(Long euronetRefNo) {
    this.put(Fields.EURONET_REFERENCE_NUMBER, euronetRefNo);
  }

  public static class FieldMapper {
    private String id;
    private String displayName;
    private String datatType;
    private Object value;
    private List<FieldMapper> fieldMapper;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getDisplayName() {
      return displayName;
    }

    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }

    public Object getValue() {
      return value;
    }

    public void setValue(Object value) {
      this.value = value;
    }

    public List<FieldMapper> getFieldMapper() {
      return fieldMapper;
    }

    public void setFieldMapper(List<FieldMapper> fieldMapper) {
      this.fieldMapper = fieldMapper;
    }

    public String getDatatType() {
      return datatType;
    }

    public void setDatatType(String datatType) {
      this.datatType = datatType;
    }

  }

  public FieldMapper getFieldMapper() {
    return fieldMapper;
  }

  public void setFieldMapper(FieldMapper fieldMapper) {
    this.fieldMapper = fieldMapper;
  }

  public List<FieldMapper> getFieldMapperList() {
    return fieldMapperList;
  }

  public void setFieldMapperList(List<FieldMapper> fieldMapperList) {
    this.fieldMapperList = fieldMapperList;
  }
}
