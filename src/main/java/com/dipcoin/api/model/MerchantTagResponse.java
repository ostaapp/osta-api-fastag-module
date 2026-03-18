package com.dipcoin.api.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MerchantTagResponse extends APIResponse{
	
	Long totalRegisteredTagCount;
	
	Double totalAmount;
	
	Double totalRegistrationAmount;
	
	Double totalMinimumAmount;

}
