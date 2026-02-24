package com.dipcoin.api.model;

import java.math.BigDecimal;

import org.apache.commons.lang3.math.NumberUtils;

import com.dipcoin.api.filter.HttpServletContext;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
@Setter
@Getter
public class TollTagsUpdateRequest extends APIRequest {
	@ApiModelProperty(required = true)
	private String serialNumber;
	
	@ApiModelProperty(required = true)
	private String registrationNo;
	
	@ApiModelProperty(required = true)
	private int vinVrnFlag;

	@Override
	public boolean validate(HttpServletContext httpServletContext) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void decrypt(HttpServletContext httpServletContext) {
		// TODO Auto-generated method stub
		
	}
	

}
