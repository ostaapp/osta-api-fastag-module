package com.dipcoin.api.model;

import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.commons.LogFormatter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

// JsonInclude annotation is to not send null values to client.
@JsonInclude(Include.NON_NULL)
@XmlAccessorType(XmlAccessType.NONE)
public class APIResponse {
  private static final Logger LOG = LogManager.getLogger(APIResponse.class);

  private static ObjectMapper objectMapper = new ObjectMapper();

  public class ResponseHeader {
    private String code;
    private String message;

    public ResponseHeader(String code, String message) {
      this.code = code;
      this.message = message;
    }

    public final String getCode() {
      return code;
    }

    public final void setCode(String code) {
      this.code = code;
    }

    public final String getMessage() {
      return message;
    }

    public final void setMessage(String message) {
      this.message = message;
    }

  }

  boolean disableHeader;

  private List<ResponseHeader> codes = null;

  public final List<ResponseHeader> getCodes() {
    if (disableHeader)
      return null;

    return codes;
  }

  public final void setCodes(List<ResponseHeader> codes) {
    this.codes = codes;
  }

  public APIResponse addHeaderCode(HeaderCode rcode) {
    if (this.codes == null)
      codes = new LinkedList<ResponseHeader>();

    this.codes.add(new ResponseHeader(rcode.code(), rcode.message()));
    return this;
  }

  // in case arguments are passed along with Header Code
  public APIResponse addHeaderCode(HeaderCode rcode, Object... args) {
    if (this.codes == null)
      codes = new LinkedList<ResponseHeader>();

    this.codes.add(new ResponseHeader(rcode.code(), String.format(rcode.message(), args)));
    return this;
  }
  
  public APIResponse addHeaderCode(String code, String message) {
	    if (this.codes == null)
	      codes = new LinkedList<ResponseHeader>();
	    
	    this.codes.add(new ResponseHeader(code, message));
	    
	    return this;
	  }

  public APIResponse addHeaderCodes(List<HeaderCode> rcodes) {
    if (rcodes != null) {
      for (HeaderCode code : rcodes) {
        this.addHeaderCode(code);
      }
    }

    return this;
  }

  public void disableHeader(boolean flag) {
    this.disableHeader = flag;
  }

  public static APIResponse error(HeaderCode header) {
    APIResponse response = new APIResponse();
    return response.addHeaderCode(header);
  }

  // in case arguments are passed with Header Code
  public static APIResponse error(HeaderCode header, Object... args) {
    APIResponse response = new APIResponse();
    return response.addHeaderCode(header, args);
  }

  public static APIResponse errors(List<HeaderCode> headers) {
    APIResponse response = new APIResponse();
    return response.addHeaderCodes(headers);
  }
  
  public APIResponse removeHeaderCodes() {
    if (this.codes == null)
      codes = new LinkedList<ResponseHeader>();

    this.codes.clear();
    return this;
  }

  // debugging purpose
  public String toString() {
    try {
      return objectMapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      LOG.error(LogFormatter.instance().message("Exception caught").format(), e);
    }
    return null;
  }

}
