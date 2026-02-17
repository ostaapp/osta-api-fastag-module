package com.dipcoin.partner.recharge.comm;

import java.util.HashMap;

public class MDMRequestDetails extends HashMap<String, Object> {

  private static final long serialVersionUID = 1L;

  public static interface Fields {
    public static String COMMON_CLASS = "CommonClass";
    public static String SEARCH_BY_DATE = "SearchByDate";
    public static String DATE = "Date";
  }

  public final MDMCommonClass getCommonClass() {
    return (MDMCommonClass) this.get(Fields.COMMON_CLASS);
  }

  public final void setCommonClass(MDMCommonClass commonClass) {
    this.put(Fields.COMMON_CLASS, commonClass);
  }
  
  public final Object getSearchByDate() {
    return this.get(Fields.SEARCH_BY_DATE);
  }

  public final void setSearchByDate(String searchByDate) {
    this.put(Fields.SEARCH_BY_DATE, searchByDate);
  }
  
  public final Object getDate() {
    return this.get(Fields.DATE);
  }

  public final void setDate(String date) {
    this.put(Fields.DATE, date);
  }
}
