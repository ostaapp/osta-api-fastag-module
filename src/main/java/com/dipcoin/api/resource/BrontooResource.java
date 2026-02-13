package com.dipcoin.api.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.apache.commons.collections4.CollectionUtils;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.APIUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.commons.TollProperties;
import com.dipcoin.api.filter.HttpServletContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTimeZone;

import com.dipcoin.api.model.*;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.TollDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.TollTagExcCodeStatus;
import com.dipcoin.db.services.commons.DBConstants.UserRoles;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.Epc;
import com.dipcoin.db.services.model.User;
import com.dipcoin.partner.toll.commons.TollConstant;
import com.dipcoin.partner.toll.commons.TollHttpsServices;
import com.dipcoin.partner.toll.commons.TollSignatureGenerationServices;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.time.DateTime;

@Component("brontooResource")
@Transactional(rollbackFor = { Exception.class, APIException.class }, propagation = Propagation.REQUIRES_NEW)
public class BrontooResource {

	private static final Logger LOG = LogManager.getLogger(BrontooResource.class);
	private final static ObjectMapper objectMapper = new ObjectMapper();
	private static final String dateFormatter = "yyyy-MM-dd hh:mm:ss";
	private static final TimeZone dateTimeZone = TimeZone.getTimeZone("Asia/Kolkata");
	private static final BigDecimal zero = new BigDecimal("0.00");
	private final static SecureRandom randomGenerator = new SecureRandom();

	@Autowired
	private TollHttpsServices tollHttpsServices;

	@Autowired
	private TollSignatureGenerationServices tollSignatureGenerationServices;

	@Autowired
	private TollProperties tollProperties;

	@Autowired
	private BankDBService bankDBService;
	
	@Autowired
	private UserDBService userDBService;
	
	@Autowired
	private TollDBService tollDBService;

	@Autowired
	@Lazy
	private HttpServletContext httpServletContext;

	public ResponseEntity syncTime(Bank bank) throws Exception {
		TollNetcSyncTimeRequest tollNetcSyncTimeRequest = new TollNetcSyncTimeRequest();
		String responseData = null;
		Head head = new Head();
		head.setVer(TollConstant.VERSION);
		SimpleDateFormat formatter = new SimpleDateFormat(TollConstant.TS_DATE_FORMAT);
		Date date = new Date(System.currentTimeMillis());
		head.setTs(formatter.format(date));
		head.setOrgId(bank.getOrgId());

		formatter = new SimpleDateFormat(TollConstant.MSG_DATE_FORMAT);
		head.setMsgId(bank.getOrgId() + formatter.format(date).toUpperCase());
		tollNetcSyncTimeRequest.setHead(head);

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
				.data(" SyncTime Request:", tollNetcSyncTimeRequest).format());

		// Create JAXB Context
		JAXBContext jaxbContext = JAXBContext.newInstance(TollNetcSyncTimeRequest.class);

		// Create Marshaller
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

		// Required formatting??
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		// Print XML String to Console
		StringWriter sw = new StringWriter();

		// Write XML to StringWriter
		jaxbMarshaller.marshal(tollNetcSyncTimeRequest, sw);

		// Verify XML Content
		String postData = sw.toString();

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data(" SyncTime Request String:", postData)
				.format());

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(postData.getBytes(StandardCharsets.UTF_8));

		ByteArrayOutputStream byteArrayOutputStream = tollSignatureGenerationServices
				.signatureGenerationServices(byteArrayInputStream, httpServletContext.getTraceId(), bank.getOrgId());

		// NPCI Active Active Setup Phase2 changes
		String ipAddress = this.tollHttpsServices.npciHealthCheckApi(httpServletContext.getTraceId());
		if (StringUtils.isEmpty(ipAddress)) {

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(HeaderCode.NETC_NPCI_SERVER_DOWN));

		}
		int port = this.tollProperties.getNetcHealthCheckPort();
		String endPoint = this.tollProperties.getSyncTimeRequestUrl();
		String url = "https://" + ipAddress + ":" + port + endPoint;

		try {
			TollHttpsServices.bankIin = bank.getIin();

			/*
			 * responseData = tollHttpsServices.send(tollProperties.getSyncTimeRequestUrl(),
			 * httpServletContext.getTraceId(), byteArrayOutputStream);
			 */

			responseData = tollHttpsServices.send(url, httpServletContext.getTraceId(), byteArrayOutputStream);

			TollHttpsServices.bankIin = StringUtils.EMPTY;
			if (responseData == null) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("sync time response is null")
						.format());
				TollNetcSyncTimeResponse tollNetcSyncTimeResponse = new TollNetcSyncTimeResponse();
				Resp resp = new Resp();
				Time time = new Time();
				time.setServerTime(formatter.format(new Date(System.currentTimeMillis() - 60000)));
				resp.setTime(time);
				tollNetcSyncTimeResponse.setHead(head);
				tollNetcSyncTimeResponse.setResp(resp);
				return ResponseEntity.status(HttpStatus.OK).body(tollNetcSyncTimeResponse);

			}
		} catch (Exception e) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Exception caught").format(), e);
			TollNetcSyncTimeResponse tollNetcSyncTimeResponse = new TollNetcSyncTimeResponse();
			Resp resp = new Resp();
			Time time = new Time();
			time.setServerTime(formatter.format(new Date(System.currentTimeMillis() - 60000)));
			resp.setTime(time);
			tollNetcSyncTimeResponse.setHead(head);
			tollNetcSyncTimeResponse.setResp(resp);
			return ResponseEntity.status(HttpStatus.OK).body(tollNetcSyncTimeResponse);
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).data(" SyncTime Response:", responseData)
				.format());

		TollNetcSyncTimeResponse tollNetcSyncTimeResponse = JAXB.unmarshal(new StringReader(responseData),
				TollNetcSyncTimeResponse.class);

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Returning toll sync time").format());

		return ResponseEntity.status(HttpStatus.OK).body(tollNetcSyncTimeResponse);
	}

	public ResponseEntity getVehicleInfo(User user, String vehicleRegistrationNo, String tagId, String tid,
			Integer regType, String bankReferenceId) {

		String refUrl = StringUtils.EMPTY;
		Bank bank = bankDBService.getBank(bankReferenceId);
		if (bank == null) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("bank is null").format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BANK_DOESNT_EXISTS));
		}

		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("calling toll sync time").format());
		try {
			TollNetcSyncTimeResponse tollNetcSyncTimeResponse = new TollNetcSyncTimeResponse();

			ResponseEntity responseEntity = this.syncTime(bank);
			if (responseEntity.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
				return responseEntity;
			}
			tollNetcSyncTimeResponse = (TollNetcSyncTimeResponse) responseEntity.getBody();

			String[] bankInfos = tollProperties.getBankInfo().split(",");

			for (String bankInfo : bankInfos) {
				String[] info = bankInfo.split("~");
				if (info[2].equalsIgnoreCase(bank.getIin())) {
					refUrl = info[3];
				}
			}

			String responseData = this.callTollNetcRequest(vehicleRegistrationNo, tagId, tid, regType, bank, refUrl,
					tollNetcSyncTimeResponse);

			if (responseData == null) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));

			}

			TollHttpsServices.bankIin = StringUtils.EMPTY;

			TollNetcDetailsResponse tollNetcDetailsResponse = JAXB.unmarshal(new StringReader(responseData),
					TollNetcDetailsResponse.class);

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Txn Id")
					.data("Txn Id", tollNetcDetailsResponse.getTxn().getId()).format());

			/*
			 * if((UserRoles.bankRoles().contains(user.getRole()) ||
			 * UserRoles.bankUserRoles().contains(user.getRole()) ||
			 * UserRoles.merchantUserRoles().contains(user.getRole()) ||
			 * UserRoles.merchantRoles().contains(user.getRole())) && !checkVehicleStatus) {
			 */
			if (UserRoles.bankRoles().contains(user.getRole()) || UserRoles.bankUserRoles().contains(user.getRole())) {

				/*
				 * LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
				 * .message("vehicle verification Bank & Merchant side") .data("User Role",
				 * user.getRole()) .data("CheckVehicleStatus", checkVehicleStatus).format());
				 */

				List<HeaderCode> headerCodes = new ArrayList<>();
				String[] errCodes = tollNetcDetailsResponse.getTxn().getResp().getVehicle().getErrCode().split(",");
				String[] respCodes = tollNetcDetailsResponse.getTxn().getResp().getRespCode().split(",");

				for (HeaderCode headerCode : HeaderCode.values()) {
					for (String errCode : errCodes) {

						if (headerCode.code().equalsIgnoreCase("N-" + errCode)) {
							headerCodes.add(headerCode);

						}
					}
					for (String respCode : respCodes) {

						if (headerCode.code().equalsIgnoreCase("N-" + respCode)) {
							headerCodes.add(headerCode);

						}
					}
				}

				TollTagResponse tollTagResponse = new TollTagResponse();
				if (tollNetcDetailsResponse.getTxn().getResp().getVehicle().getVehicleDetails() != null
						&& CollectionUtils.isNotEmpty(
								tollNetcDetailsResponse.getTxn().getResp().getVehicle().getVehicleDetails())) {
					String excCode = StringUtils.EMPTY;
					String issueDate = StringUtils.EMPTY;
					for (VehicleDetails vehicleDetail : tollNetcDetailsResponse.getTxn().getResp().getVehicle()
							.getVehicleDetails()) {

						for (Detail detail : vehicleDetail.getDetail()) {

							if (detail.getName().equalsIgnoreCase(TollConstant.EXCCODE)) {
								excCode = excCode + detail.getValue() + ",";
								LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
										.message("Vehicle Response Exc data").data("Exc Code", excCode).format());
							}

							if (detail.getName().equalsIgnoreCase(TollConstant.ISSUEDATE)) {
								issueDate = detail.getValue();
							}

							String[] excCodes = excCode.split(",");

							for (String code : excCodes) {

								if (code.equalsIgnoreCase(TollConstant.EXC_CODE_ACTIVE)
										|| code.equalsIgnoreCase(TollConstant.EXC_CODE_LOWBALANCE_LIST)
										|| code.equalsIgnoreCase(TollConstant.EXC_CODE_INVALID_CARRIAGE)
										|| code.equalsIgnoreCase(TollConstant.EXC_CODE_EXEMTED_LIST)) {

									LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
											.message("Vehicle Response data").data("Exc Codes", excCodes)
											.data("Issue Date", issueDate).data("Exc Code", code).format());

									if (StringUtils.isNotEmpty(issueDate)) {

										SimpleDateFormat format = new SimpleDateFormat(
												TollConstant.TAG_VERIFICATION_DATE_FORMAT);
										long tagIssueTime = format.parse(issueDate).getTime();

										DateTime minTagIssueTTL = APIUtils.findStartTimeOfDay(tagIssueTime,
												DBConstants.CHARGE_BACK_MIN_TTL_HRS);

										LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
												.message("Tag issue date details")
												.data("Tag Issue Date", detail.getValue())
												.data("Current day",
														APIUtils.findStartTimeOfDay(
																DateTime.now(DateTimeZone.UTC).getMillis()))
												.data("ExcCode", excCode).format());

										// can only raise dispute after 24 of the transaction
										if (!APIUtils.findStartTimeOfDay(DateTime.now(DateTimeZone.UTC).getMillis())
												.isAfter(minTagIssueTTL)) {

											if (StringUtils.isNotBlank(excCode)) {
												excCode = excCode.substring(NumberUtils.INTEGER_ZERO,
														excCode.length() - NumberUtils.INTEGER_ONE);
												tollTagResponse.setExcCode(excCode);
											}

											LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
													.message("Duplicate registratation on same day")
													.data("Tag Issue Date", detail.getValue())
													.data("Current day time",
															APIUtils.findStartTimeOfDay(
																	DateTime.now(DateTimeZone.UTC).getMillis()))
													.data("ExcCode", excCode).format());

											return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
													APIResponse.error(HeaderCode.DUPLICATE_REGISTRATION_ON_SAME_DAY));
										}

									}
								}

							}

						}
					}
					if (StringUtils.isNotBlank(excCode)) {
						excCode = excCode.substring(NumberUtils.INTEGER_ZERO,
								excCode.length() - NumberUtils.INTEGER_ONE);
						tollTagResponse.setExcCode(excCode);
					}
				}

				tollTagResponse.addHeaderCodes(headerCodes);

				return ResponseEntity.status(HttpStatus.OK).body(tollTagResponse);

			}

			if (userDBService.isBrontooSuperAdmin(user)) {
				return ResponseEntity.status(HttpStatus.OK).body(
						objectMapper.readValue(objectMapper.writeValueAsString(tollNetcDetailsResponse), Map.class));
			}

			// Get all banks name & Iin from property file
			String[] banks = tollProperties.getBankNameAndIin().split(",");

			List<CustomerBankVehicleVerificationResponse> response = new ArrayList<>();

			for (VehicleDetails vehicleDetail : tollNetcDetailsResponse.getTxn().getResp().getVehicle()
					.getVehicleDetails()) {

				for (Detail detail : vehicleDetail.getDetail()) {

					if (detail.getName().equalsIgnoreCase(TollConstant.BANK_ID)) {

						for (String bankInfo : banks) {

							if (bankInfo.contains(detail.getValue())
									&& bank.getIin().equalsIgnoreCase(detail.getValue())) {

								CustomerBankVehicleVerificationResponse vehicleVerificationResponse = new CustomerBankVehicleVerificationResponse();

								vehicleVerificationResponse.setBankName(bankInfo.split("~")[0]);

								for (Detail tagDetail : vehicleDetail.getDetail()) {

									if (tagDetail.getName().equalsIgnoreCase(TollConstant.TAGID)) {

										Epc epc = tollDBService.findEpcByTagId(tagDetail.getValue());

										vehicleVerificationResponse.setSerialNumber(
												epc != null ? epc.getSerialNumber() : StringUtils.EMPTY);

									}

									if (tagDetail.getName().equalsIgnoreCase(TollConstant.REGNUMBER)) {
										vehicleVerificationResponse.setRegNumber(tagDetail.getValue());
									}

									if (tagDetail.getName().equalsIgnoreCase(TollConstant.VEHICLECLASS)) {
										vehicleVerificationResponse.setVehicleClass(tagDetail.getValue());
									}

									if (tagDetail.getName().equalsIgnoreCase(TollConstant.TAG_STATUS)) {
										vehicleVerificationResponse.setTagStatus(tagDetail.getValue());
									}

									if (tagDetail.getName().equalsIgnoreCase(TollConstant.ISSUEDATE)) {
										vehicleVerificationResponse.setIssueDate(tagDetail.getValue());
									}

									if (tagDetail.getName().equalsIgnoreCase(TollConstant.EXCCODE)) {

										String excCode = StringUtils.EMPTY;

										// If Vehicle has more than one ExcCode status. e.g, Low Balance & BlackListed
										if (tagDetail.getValue().contains(",")) {

											String[] excCodes = tagDetail.getValue().split(",");

											for (String code : excCodes) {
												if (code.equalsIgnoreCase(TollConstant.EXC_CODE_ACTIVE)) {

													excCode = (StringUtils.isEmpty(excCode))
															? TollTagExcCodeStatus.ACTIVE.name()
															: excCode + ", " + TollTagExcCodeStatus.ACTIVE.name();

												}
												if (code.equalsIgnoreCase(TollConstant.EXC_CODE_BLACKLIST)) {

													excCode = (StringUtils.isEmpty(excCode))
															? TollTagExcCodeStatus.BLACKLIST.name()
															: excCode + ", " + TollTagExcCodeStatus.BLACKLIST.name();

												}
												if (code.equalsIgnoreCase(TollConstant.EXC_CODE_EXEMTED_LIST)) {

													excCode = (StringUtils.isEmpty(excCode))
															? TollTagExcCodeStatus.EXEMPTED_LIST.name()
															: excCode + ", "
																	+ TollTagExcCodeStatus.EXEMPTED_LIST.name();

												}
												if (code.equalsIgnoreCase(TollConstant.EXC_CODE_LOWBALANCE_LIST)) {

													excCode = (StringUtils.isEmpty(excCode))
															? TollTagExcCodeStatus.LOW_BALANCE.name()
															: excCode + ", " + TollTagExcCodeStatus.LOW_BALANCE.name();
												}
												if (code.equalsIgnoreCase(TollConstant.EXC_CODE_HOTLIST)) {

													excCode = (StringUtils.isEmpty(excCode))
															? TollTagExcCodeStatus.HOTLIST.name()
															: excCode + ", " + TollTagExcCodeStatus.HOTLIST.name();
												}
												if (code.equalsIgnoreCase(TollConstant.EXC_CODE_INVALID_CARRIAGE)) {

													excCode = (StringUtils.isEmpty(excCode))
															? TollTagExcCodeStatus.INVALID_CARRIAGE.name()
															: excCode + ", "
																	+ TollTagExcCodeStatus.INVALID_CARRIAGE.name();
												}
											}

										} else {

											excCode = (tagDetail.getValue()
													.equalsIgnoreCase(TollConstant.EXC_CODE_ACTIVE))
															? TollTagExcCodeStatus.ACTIVE.name()
															: (tagDetail.getValue()
																	.equalsIgnoreCase(TollConstant.EXC_CODE_BLACKLIST))
																			? TollTagExcCodeStatus.BLACKLIST.name()
																			: (tagDetail.getValue().equalsIgnoreCase(
																					TollConstant.EXC_CODE_EXEMTED_LIST))
																							? TollTagExcCodeStatus.EXEMPTED_LIST
																									.name()
																							: (tagDetail.getValue()
																									.equalsIgnoreCase(
																											TollConstant.EXC_CODE_HOTLIST))
																													? TollTagExcCodeStatus.HOTLIST
																															.name()
																													: (tagDetail
																															.getValue()
																															.equalsIgnoreCase(
																																	TollConstant.EXC_CODE_CLOSED_OR_REPLACED))
																																			? TollTagExcCodeStatus.CLOSED_OR_REPLACED
																																					.name()
																																			: TollTagExcCodeStatus.LOW_BALANCE
																																					.name();
										}

										vehicleVerificationResponse.setExcCode(excCode);

									}

									if (tagDetail.getName().equalsIgnoreCase(TollConstant.COMVEHICLE)) {
										String vehicleType = (tagDetail.getValue().equalsIgnoreCase("F")) ? "PRIVATE"
												: "COMMERCIAL";

										vehicleVerificationResponse.setVehicleType(vehicleType);
									}

								}

								response.add(vehicleVerificationResponse);
							}

						}
					}
				}
			}

			return ResponseEntity.status(HttpStatus.OK).body(response);

		} catch (Exception ex) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("vehicle verification" + ex)
					.format());
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(HeaderCode.BAD_REQUEST));

	}

	//
	public String callTollNetcRequest(String vehicleRegistrationNo, String tagId, String tid, Integer regType,
			Bank bank, String refUrl, TollNetcSyncTimeResponse tollNetcSyncTimeResponse) throws Exception {

		Vehicle vehicle = new Vehicle();

		vehicle.setAvc(TollConstant.EMPTY);
		vehicle.setVehicleRegNo(TollConstant.EMPTY);
		vehicle.setTagId(TollConstant.EMPTY);
		vehicle.setTID(TollConstant.EMPTY);

		if (StringUtils.isNotBlank(vehicleRegistrationNo)) {
			vehicle.setVehicleRegNo(vehicleRegistrationNo.toUpperCase());
		} else if (StringUtils.isNotBlank(tagId)) {
			vehicle.setTagId(tagId.toUpperCase());
		} else if (StringUtils.isNotBlank(tid)) {
			vehicle.setTID(tid.toUpperCase());
		}

		String dipcoinReferenceNumber = CoreUtils.randomAlphaString(22);

		Txn txn = new Txn();

		Head head = new Head();
		// head.setVer(TollConstant.VERSION);
		head.setVer(TollConstant.VER);
		SimpleDateFormat formatter = new SimpleDateFormat(TollConstant.TS_DATE_FORMAT);
		Date date = formatter.parse(tollNetcSyncTimeResponse.getResp().getTs());
		head.setTs(tollNetcSyncTimeResponse.getResp().getTs());
		txn.setTs(tollNetcSyncTimeResponse.getResp().getTs());
		head.setOrgId(bank.getOrgId());

		formatter = new SimpleDateFormat(TollConstant.MSG_DATE_FORMAT);
		date = new Date(System.currentTimeMillis());
		head.setMsgId(bank.getOrgId() + formatter.format(date).toUpperCase());

		txn.setId(dipcoinReferenceNumber);
		txn.setNote(TollConstant.REQUEST_DETAILS_NOTE);
		txn.setOrgTxnId(bank.getOrgId() + dipcoinReferenceNumber);
		txn.setRefId(dipcoinReferenceNumber);
		txn.setRefUrl(refUrl);
		txn.setType(TollConstant.REQUEST_DETAILS_TYPE);
		txn.setVehicle(vehicle);

		TollNetcDetailsRequest tollNetcDetailsRequest = new TollNetcDetailsRequest();

		tollNetcDetailsRequest.setHead(head);
		tollNetcDetailsRequest.setTxn(txn);

		// Create JAXB Context
		JAXBContext jaxbContext = JAXBContext.newInstance(TollNetcDetailsRequest.class);

		// Create Marshaller
		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

		// Required formatting??
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

		// Print XML String to Console
		StringWriter sw = new StringWriter();

		// Write XML to StringWriter
		jaxbMarshaller.marshal(tollNetcDetailsRequest, sw);

		// Verify XML Content
		String postData = sw.toString();

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(postData.getBytes(StandardCharsets.UTF_8));

		ByteArrayOutputStream byteArrayOutputStream = tollSignatureGenerationServices
				.signatureGenerationServices(byteArrayInputStream, httpServletContext.getTraceId(), bank.getOrgId());
		String responseData = null;

		// NPCI Active Active Setup Phase2 changes
		String ipAddress = this.tollHttpsServices.npciHealthCheckApi(httpServletContext.getTraceId());
		if (StringUtils.isEmpty(ipAddress)) {

			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("********** NPCI Server is DOWN **********").format());
			return null;

		}
		int port = this.tollProperties.getNetcHealthCheckPort();

		try {
			if (tollProperties.isConnectNpci()) {
				TollHttpsServices.bankIin = bank.getIin();
				/*
				 * String url = "";
				 * 
				 * switch (regType) { case 0: url = tollProperties.getRequestDetailUrl(); break;
				 * case 1: url = tollProperties.getRequestDetailIHMCL(); break; default: url =
				 * tollProperties.getRequestDetailUrl(); break; }
				 */

				String endPoint = "";
				switch (regType) {
				case 0:
					endPoint = tollProperties.getRequestDetailUrl();
					break;
				case 1:
					endPoint = tollProperties.getRequestDetailIHMCL();
					break;
				default:
					endPoint = tollProperties.getRequestDetailUrl();
					break;
				}

				String url = "https://" + ipAddress + ":" + port + endPoint;

				responseData = tollHttpsServices.send(url, httpServletContext.getTraceId(), byteArrayOutputStream);

				return responseData;
			}
		} catch (Exception e) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
					.message("Exception Caught while calling tollHttpsServices").format(), e);
		}
		return null;

	}

}
