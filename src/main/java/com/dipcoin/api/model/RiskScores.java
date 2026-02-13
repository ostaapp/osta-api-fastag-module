package com.dipcoin.api.model;

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.PROPERTY)
public class RiskScores {

  private List<Score> score;

  @XmlElement(name = "Score")
  public List<Score> getScore() {
    return score;
  }

  public void setScore(List<Score> score) {
    this.score = score;
  }

}
