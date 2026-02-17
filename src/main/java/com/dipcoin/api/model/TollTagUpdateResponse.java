package com.dipcoin.api.model;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "RespMngTagEntries", namespace = "http://npci.org/etc/schema/")
@XmlAccessorType(XmlAccessType.FIELD)
public class TollTagUpdateResponse extends APIResponse {

  @XmlAccessorType(XmlAccessType.PROPERTY)
  public static class Head {


    private String ver;


    private String ts;


    private String orgId;


    private String msgId;



    @XmlAttribute
    public String getVer() {
      return ver;
    }

    public void setVer(String ver) {
      this.ver = ver;
    }

    @XmlAttribute
    public String getTs() {
      return ts;
    }

    public void setTs(String ts) {
      this.ts = ts;
    }

    @XmlAttribute
    public String getOrgId() {
      return orgId;
    }

    public void setOrgId(String orgId) {
      this.orgId = orgId;
    }

    @XmlAttribute
    public String getMsgId() {
      return msgId;
    }

    public void setMsgId(String msgId) {
      this.msgId = msgId;
    }

  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Txn {

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Resp {

      @XmlAccessorType(XmlAccessType.FIELD)
      public static class Tag {

        @XmlAttribute
        private String op;

        @XmlAttribute
        private String tagId;

        @XmlAttribute
        private String seqNum;

        @XmlAttribute
        private String result;

        @XmlAttribute
        private String errCode;

        public String getOp() {
          return op;
        }

        public void setOp(String op) {
          this.op = op;
        }

        public String getTagId() {
          return tagId;
        }

        public void setTagId(String tagId) {
          this.tagId = tagId;
        }

        public String getSeqNum() {
          return seqNum;
        }

        public void setSeqNum(String seqNum) {
          this.seqNum = seqNum;
        }

        public String getResult() {
          return result;
        }

        public void setResult(String result) {
          this.result = result;
        }

        public String getErrCode() {
          return errCode;
        }

        public void setErrCode(String errCode) {
          this.errCode = errCode;
        }
      }

      @XmlElement(name = "Tag")
      private List<Tag> tag;

      @XmlAttribute
      private String ts;

      @XmlAttribute
      private String result;

      @XmlAttribute
      private String respCode;

      @XmlAttribute
      private String totReqCnt;

      @XmlAttribute
      private String sucessReqCnt;

      public List<Tag> getTag() {
        return tag;
      }

      public void setTag(List<Tag> tag) {
        this.tag = tag;
      }

      public String getTs() {
        return ts;
      }

      public void setTs(String ts) {
        this.ts = ts;
      }

      public String getResult() {
        return result;
      }

      public void setResult(String result) {
        this.result = result;
      }

      public String getRespCode() {
        return respCode;
      }

      public void setRespCode(String respCode) {
        this.respCode = respCode;
      }

      public String getTotReqCnt() {
        return totReqCnt;
      }

      public void setTotReqCnt(String totReqCnt) {
        this.totReqCnt = totReqCnt;
      }

      public String getSucessReqCnt() {
        return sucessReqCnt;
      }

      public void setSucessReqCnt(String sucessReqCnt) {

        this.sucessReqCnt = sucessReqCnt;
      }
    }

    @XmlElement(name = "Resp")
    private Resp resp;

    @XmlAttribute
    private String Id;

    @XmlAttribute
    private String note;

    @XmlAttribute
    private String refId;

    @XmlAttribute
    private String refUrl;

    @XmlAttribute
    private String ts;

    @XmlAttribute
    private String type;

    @XmlAttribute
    private String orgTxnId;

    public Resp getResp() {
      return resp;
    }

    public void setResp(Resp resp) {
      this.resp = resp;
    }

    public String getId() {
      return Id;
    }

    public void setId(String id) {
      Id = id;
    }

    public String getNote() {
      return note;
    }

    public void setNote(String note) {
      this.note = note;
    }

    public String getRefId() {
      return refId;
    }

    public void setRefId(String refId) {
      this.refId = refId;
    }

    public String getRefUrl() {
      return refUrl;
    }

    public void setRefUrl(String refUrl) {
      this.refUrl = refUrl;
    }

    public String getTs() {
      return ts;
    }

    public void setTs(String ts) {
      this.ts = ts;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getOrgTxnId() {
      return orgTxnId;
    }

    public void setOrgTxnId(String orgTxnId) {
      this.orgTxnId = orgTxnId;
    }

  }

  @XmlElement(name = "Head")
  private Head head;

  @XmlElement(name = "Txn")
  private Txn txn;

  public Head getHead() {
    return head;
  }

  public void setHead(Head head) {
    this.head = head;
  }

  public Txn getTxn() {
    return txn;
  }

  public void setTxn(Txn txn) {
    this.txn = txn;
  }



}
