package com.dipcoin.api.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.Setter;

@JsonInclude(Include.NON_NULL)
@Setter
@Getter
public class ReqVehicleDetailResponse {
	private static final Logger LOG = LogManager.getLogger(ReqVehicleDetailResponse.class);

	private String TAGID;
	private String REGNUMBER;
	private String TID;
	private String VIN;
	private String ENGINENO;
	private String STATE;
	private String VEHICLEDESCRIPTOR;
	private String REGISTERED_VEHICLE;
	private String MAKER_DESCR;
	private String MONTH_YEAR;
	private String NUMBER_OF_AXLES;
	private String F_AXLE_DESCP;
	private String R_AXLE_DESCP;
	private String T_AXLE_DESCP;
	private String O_AXLE_DESCP;
	private String F_AXLE_WEIGHT;
	private String R_AXLE_WEIGHT;
	private String T_AXLE_WEIGHT;
	private String O_AXLE_WEIGHT;
	private String MAKE_MODEL;
	private String NUMBER_OF_SEATS;
	private String COLOR;
	private String FUEL_DESCR;
	private String UNLD_WT;
	private String GVW;
	private String NATIONAL_PERMIT;
	private String NATIONAL_PERMIT_START_DATE;
	private String NATIONAL_PERMIT_END_DATE;
	private String ALL_INDIA_TOURIST_PERMIT;
	private String ALL_INDIA_TOURIST_PERMIT_START_DATE;
	private String ALL_INDIA_TOURIST_PERMIT_END_DATE;
}
