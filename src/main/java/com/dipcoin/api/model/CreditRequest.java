package com.dipcoin.api.model;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;

import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.db.services.model.CustomerAccount;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class CreditRequest {
	
	  @ApiModelProperty(required = true)
	  private BigDecimal amount = BigDecimal.ONE;
	  @ApiModelProperty(required = true)
	  private int customerAccountId;
	  @ApiModelProperty(required = true)
      private String usedAt;
	  
	 

}
