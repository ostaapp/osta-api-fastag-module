package com.dipcoin.api.model;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class TagList {

  private List<Tag> tag;

  @XmlElement(name = "Tag")
  public List<Tag> getTag() {
    return tag;
  }

  public void setTag(List<Tag> tag) {
    this.tag = tag;
  }
}
