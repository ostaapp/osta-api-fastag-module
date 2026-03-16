package com.dipcoin.api.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.dipcoin.api.commons.APIException;
import com.dipcoin.api.commons.APIUtils;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.commons.OfflineJobClient;
import com.dipcoin.api.commons.TollEmailUtils;
import com.dipcoin.api.commons.TollProperties;
import com.dipcoin.api.config.ApplicationProperties;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
import com.dipcoin.api.model.CustomerBankVehicleVerificationResponse;
import com.dipcoin.api.model.Detail;
import com.dipcoin.api.model.Head;
import com.dipcoin.api.model.Resp;
import com.dipcoin.api.model.Tag;
import com.dipcoin.api.model.TagList;
import com.dipcoin.api.model.Time;
import com.dipcoin.api.model.TollMngTagExceptionRequest;
import com.dipcoin.api.model.TollMngTagExceptionResponse;
import com.dipcoin.api.model.TollNetcDetailsRequest;
import com.dipcoin.api.model.TollNetcDetailsResponse;
import com.dipcoin.api.model.TollNetcSyncTimeRequest;
import com.dipcoin.api.model.TollNetcSyncTimeResponse;
import com.dipcoin.api.model.TollTagRequest;
import com.dipcoin.api.model.TollTagResponse;
import com.dipcoin.api.model.Txn;
import com.dipcoin.api.model.Vehicle;
import com.dipcoin.api.model.VehicleDetails;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.commons.SmsClient;
import com.dipcoin.commons.SmsClient.Templates;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.DipcoinDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.TollDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.commons.DBConstants;
import com.dipcoin.db.services.commons.DBConstants.MerchantBusinessSegment;
import com.dipcoin.db.services.commons.DBConstants.TollTagExcCodeStatus;
import com.dipcoin.db.services.commons.DBConstants.UserRoles;
import com.dipcoin.db.services.model.Bank;
import com.dipcoin.db.services.model.BankTransaction;
import com.dipcoin.db.services.model.Dipcoin;
import com.dipcoin.db.services.model.DipcoinTransaction;
import com.dipcoin.db.services.model.Epc;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.TollTag;
import com.dipcoin.db.services.model.User;
import com.dipcoin.notification.services.model.NotificationRequestContext;
import com.dipcoin.partner.toll.commons.TollConstant;
import com.dipcoin.partner.toll.commons.TollHttpsServices;
import com.dipcoin.partner.toll.commons.TollSignatureGenerationServices;
import com.fasterxml.jackson.databind.ObjectMapper;


@Component("brontooResource")
@Transactional(rollbackFor = {Exception.class, APIException.class},
    propagation = Propagation.REQUIRES_NEW)
public class BrontooResource {

  private static final Logger LOG = LogManager.getLogger(BrontooResource.class);
  private final static ObjectMapper objectMapper = new ObjectMapper();
  
  @Autowired
  private MerchantDBService merchantDBService;

  @Autowired
  private UserDBService userDBService;

  @Autowired
  private BankDBService bankDBService;

  @Autowired
  @Lazy
  private HttpServletContext httpServletContext;

  @Autowired
  private TollHttpsServices tollHttpsServices;

  @Autowired
  private TollSignatureGenerationServices tollSignatureGenerationServices;

  @Autowired
  private TollDBService tollDBService;

  @Autowired
  private TollProperties tollProperties; 

  @Autowired
  private ApplicationProperties applicationProperties;
  
  @Autowired
  private TollEmailUtils tollEmailUtils;
  
  @Autowired
  private SmsClient smsClient;

  @Autowired
  private NotificationResource notificationResource;
  
  @Autowired
  CustomerDipcoinResource customerDipcoinResource;
  
  @Autowired
  private DipcoinDBService coinDBService;
  
  @Autowired
  private OfflineJobClient offlineJobClient;
  
  @Autowired
  @Qualifier("debitsReqpayRabbitTemplate")
  private RabbitTemplate debitsRabbitTemplate;

 
  public void setHttpServletContext(HttpServletContext httpServletContext) {
    this.httpServletContext = httpServletContext;
  }

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
	
	public ResponseEntity updateExceptionList(TollTag tollTag_obj, String operation,
			final TollTagRequest tollTagRequest) throws Exception, APIException {
	  
	  /*
      LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("In updateExceptionList method").data("tollTag_obj", tollTag_obj)
          .data("operation", operation).data("TollTagRequest", tollTagRequest).format());
      */
	  
		TollMngTagExceptionRequest tollMngTagExceptionRequest = new TollMngTagExceptionRequest();
		TollMngTagExceptionResponse tollMngTagExceptionResponse = new TollMngTagExceptionResponse();
		List<TollMngTagExceptionResponse> tollMngTagExceptionResponseList = new ArrayList<>();
		String refUrl = StringUtils.EMPTY;

		List<Merchant> merchants = merchantDBService
				.asyncFindMerchantByBusinessSegment(MerchantBusinessSegment.TOLL.value()).get();

		if (CollectionUtils.isEmpty(merchants)) {
			LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("merchant is null").format());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(APIResponse.error(HeaderCode.MERCHANT_DOESNT_EXIST));
		}

		List<TollTag> tollTags = tollTag_obj != null ? Arrays.asList(tollTag_obj)
				: tollDBService.findTollTagByStatusOrderByBankId(
						String.valueOf(DBConstants.TollTagApprovalStatus.ACTIVE.value()));

		Set<Integer> bankIds = new HashSet<Integer>();
		
		/*
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
	          .message("In updateExceptionList before for loop").data("tollTags", tollTags).format());
        */
		
		for (TollTag tollTag : tollTags) {
			if(tollTag.getWalletBankId()> NumberUtils.INTEGER_ZERO){
				bankIds.add(tollTag.getWalletBankId());
			} else {
				bankIds.add(tollTag.getBankId());
			}
			
		}
		
		/*
		LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("In updateExceptionList after for loop").data("bankIds", bankIds).format());
        */

		if (!CollectionUtils.isEmpty(tollTags)) {

			for (Integer bankId : bankIds) {
				Bank bank = bankDBService.getBank(bankId);
				if (bank != null) {
					ResponseEntity responseEntity = syncTime(bank);

					if (responseEntity.getStatusCodeValue() >= HttpStatus.BAD_REQUEST.value()) {
						return responseEntity;
					}

					TollNetcSyncTimeResponse tollNetcSyncTimeResponse = (TollNetcSyncTimeResponse) responseEntity
							.getBody();

					String[] bankInfos = tollProperties.getBankInfo().split(",");

					for (String bankInfo : bankInfos) {
						String[] info = bankInfo.split("~");
						if (info[2].equalsIgnoreCase(bank.getIin())) {
							refUrl = info[3];
						}
					}

					Txn txn = new Txn();

					Head head = new Head();
					head.setVer(TollConstant.VERSION);
					SimpleDateFormat formatter = new SimpleDateFormat(TollConstant.TS_DATE_FORMAT);
					Date date = formatter.parse(tollNetcSyncTimeResponse.getResp().getTs());
					head.setTs(formatter.format(date));
					txn.setTs(formatter.format(date));
					head.setOrgId(bank.getOrgId());

					formatter = new SimpleDateFormat(TollConstant.MSG_DATE_FORMAT);
					date = new Date(System.currentTimeMillis());
					head.setMsgId(bank.getOrgId() + formatter.format(date).toUpperCase());

					String dipcoinReferenceNumber = CoreUtils.randomAlphaString(22);

					txn.setId(dipcoinReferenceNumber);
					txn.setNote(TollConstant.MANAGE_EXCEPTION_NOTE);
					txn.setOrgTxnId(bank.getOrgId() + dipcoinReferenceNumber);
					txn.setRefId(dipcoinReferenceNumber);
					txn.setRefUrl(refUrl);
					//txn.setType(TollConstant.MANAGE_EXCEPTION_TYPE);
                    
					txn.setType(TollConstant.MANAGE_EXCEPTION_TYPE);
					

					TagList tagList = new TagList();

					List<Tag> tags = new ArrayList<>();
					Integer seqNum = NumberUtils.INTEGER_ZERO;
					for (TollTag tollTag : tollTags) {

						if ((httpServletContext.isProdEnvironment()
								&& tollTag.getExcCode().equals(TollConstant.EXC_CODE_EXEMTED_LIST))
								|| (tollTag.getWalletBankId() > NumberUtils.INTEGER_ZERO ? tollTag.getWalletBankId() != bankId : tollTag.getBankId() != bankId)
								|| (tollTagRequest != null && StringUtils.isNotBlank(tollTagRequest.getExcCode())
										&& ((httpServletContext.isProdEnvironment() && tollTagRequest.getExcCode()
												.equals(TollConstant.EXC_CODE_EXEMTED_LIST))
												|| !tollTag.getExcCode()
														.equalsIgnoreCase(tollTagRequest.getExcCode())))) {
							continue;
						}

						Tag tag = new Tag();
						tag.setExcCode(tollTag.getExcCode());
						tag.setOp(operation.equals(TollConstant.ADD_OP) || operation.equals(TollConstant.ADD_TO_NPCI)
								? TollConstant.ADD_OP
								: TollConstant.REMOVE_OP);
						tag.setSeqNum(String.valueOf(++seqNum));
						tag.setTagId(tollTag.getTagId());
						tags.add(tag);

						if (operation.equals(TollConstant.ADD_OP) || operation.equals(TollConstant.REMOVE_OP)) {
							try {
								if (tollTag.getExcCode().equalsIgnoreCase(TollConstant.EXC_CODE_BLACKLIST)
										&& operation.equalsIgnoreCase(TollConstant.ADD_OP)) {

									// success sms
									if (!applicationProperties.getAwsSMSClient()
											&& !smsClient.sendSms(tollTag.getTollRegistration().getMobileNo(),
													Templates.TollTagBlackListing.format(bank.getAlias()), true)) {
										LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
												.message("Failed to send SMS")
												.data("phone", tollTag.getTollRegistration().getMobileNo()).format());
									}

									if (applicationProperties.getAwsSMSClient()) {

										NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
										notificationRequestContext.setTraceId(httpServletContext.getTraceId());
										if (!notificationResource.sendSms(tollTag.getTollRegistration().getMobileNo(),
												Templates.TollTagBlackListing.format(bank.getAlias()),
												httpServletContext.getClientFeatureFlags().smsEnabled(),
												notificationRequestContext)) {

											LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
													.message("Failed to send SMS")
													.data("phone", tollTag.getTollRegistration().getMobileNo())
													.format());

										}
									}

									// Email
									if (!this.tollEmailUtils.tollTagBlackListing(tollTag.getRegistrationNo(), bank,
											tollTag.getTollRegistration())) {
										LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
												.message("Failed to send email to Blacklisting of tag.")
												.data("user email", tollTag.getTollRegistration().getEmailId())
												.format());
									}

								} else if (tollTag.getExcCode().equalsIgnoreCase(TollConstant.EXC_CODE_BLACKLIST)
										&& operation.equalsIgnoreCase(TollConstant.REMOVE_OP)) {

									// success sms
									if (!applicationProperties.getAwsSMSClient()
											&& !smsClient.sendSms(tollTag.getTollRegistration().getMobileNo(),
													Templates.TollTagWhiteListing.format(bank.getAlias()), true)) {
										LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
												.message("Failed to send SMS")
												.data("phone", tollTag.getTollRegistration().getMobileNo()).format());
									}

									if (applicationProperties.getAwsSMSClient()) {

										NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
										notificationRequestContext.setTraceId(httpServletContext.getTraceId());
										if (!notificationResource.sendSms(tollTag.getTollRegistration().getMobileNo(),
												Templates.TollTagWhiteListing.format(bank.getAlias()),
												httpServletContext.getClientFeatureFlags().smsEnabled(),
												notificationRequestContext)) {

											LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
													.message("Failed to send SMS")
													.data("phone", tollTag.getTollRegistration().getMobileNo())
													.format());

										}
									}

									// Email
									if (!this.tollEmailUtils.tollTagWhiteListing(tollTag.getRegistrationNo(), bank,
											tollTag.getTollRegistration())) {
										LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
												.message("Failed to send email to Blacklisting of tag.")
												.data("user email", tollTag.getTollRegistration().getEmailId())
												.format());
									}

								} else if (tollTag.getExcCode().equalsIgnoreCase(TollConstant.EXC_CODE_LOWBALANCE_LIST)
										&& operation.equalsIgnoreCase(TollConstant.ADD_OP)) {
									// sms
									if (!applicationProperties.getAwsSMSClient()
											&& !smsClient.sendSms(tollTag.getTollRegistration().getMobileNo(),
													Templates.TollTagLowBalance.format(bank.getAlias()), true)) {
										LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
												.message("Failed to send SMS")
												.data("phone", tollTag.getTollRegistration().getMobileNo()).format());
									}

									if (applicationProperties.getAwsSMSClient()) {

										NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
										notificationRequestContext.setTraceId(httpServletContext.getTraceId());
										if (!notificationResource.sendSms(tollTag.getTollRegistration().getMobileNo(),
												Templates.TollTagLowBalance.format(bank.getAlias()),
												httpServletContext.getClientFeatureFlags().smsEnabled(),
												notificationRequestContext)) {

											LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
													.message("Failed to send SMS")
													.data("phone", tollTag.getTollRegistration().getMobileNo())
													.format());

										}
									}

									// Email
									if (!this.tollEmailUtils.tollTagLowBalance(tollTag.getRegistrationNo(), bank,
											tollTag.getTollRegistration())) {
										LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
												.message("Failed to send email to set tag low balance.")
												.data("user email", tollTag.getTollRegistration().getEmailId())
												.format());
									}

								}
								else if (tollTag.getExcCode().equalsIgnoreCase(TollConstant.EXC_CODE_HOTLIST)
                                    && operation.equalsIgnoreCase(TollConstant.ADD_OP)) {
                                // sms
                                if (!applicationProperties.getAwsSMSClient()
                                        && !smsClient.sendSms(tollTag.getTollRegistration().getMobileNo(),
                                                Templates.TollTagHotListing.format(bank.getAlias()), true)) {
                                    LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                                            .message("Failed to send SMS")
                                            .data("phone", tollTag.getTollRegistration().getMobileNo()).format());
                                }

                                if (applicationProperties.getAwsSMSClient()) {

                                    NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
                                    notificationRequestContext.setTraceId(httpServletContext.getTraceId());
                                    if (!notificationResource.sendSms(tollTag.getTollRegistration().getMobileNo(),
                                            Templates.TollTagHotListing.format(bank.getAlias()),
                                            httpServletContext.getClientFeatureFlags().smsEnabled(),
                                            notificationRequestContext)) {

                                        LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                                                .message("Failed to send SMS")
                                                .data("phone", tollTag.getTollRegistration().getMobileNo())
                                                .format());

                                    }
                                }

                                // Email
                                if (!this.tollEmailUtils.tollTagHotListing(tollTag.getRegistrationNo(), bank,
                                        tollTag.getTollRegistration())) {
                                    LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                                            .message("Failed to send email to set tag hot listing.")
                                            .data("user email", tollTag.getTollRegistration().getEmailId())
                                            .format());
                                }

                              } else if (tollTag.getExcCode()
                                  .equalsIgnoreCase(TollConstant.EXC_CODE_HOTLIST)
                                  && operation.equalsIgnoreCase(TollConstant.REMOVE_OP)) {

                                // success sms
                                if (!applicationProperties.getAwsSMSClient() && !smsClient.sendSms(
                                    tollTag.getTollRegistration().getMobileNo(),
                                    Templates.TollTagWhiteListing.format(bank.getAlias()), true)) {
                                  LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                                      .message("Failed to send SMS")
                                      .data("phone", tollTag.getTollRegistration().getMobileNo())
                                      .format());
                                }

                                if (applicationProperties.getAwsSMSClient()) {

                                  NotificationRequestContext notificationRequestContext =
                                      new NotificationRequestContext();
                                  notificationRequestContext
                                      .setTraceId(httpServletContext.getTraceId());
                                  if (!notificationResource.sendSms(
                                      tollTag.getTollRegistration().getMobileNo(),
                                      Templates.TollTagWhiteListing.format(bank.getAlias()),
                                      httpServletContext.getClientFeatureFlags().smsEnabled(),
                                      notificationRequestContext)) {

                                    LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                                        .message("Failed to send SMS")
                                        .data("phone", tollTag.getTollRegistration().getMobileNo())
                                        .format());

                                  }
                                }

                                // Email
                                if (!this.tollEmailUtils.tollTagWhiteListing(
                                    tollTag.getRegistrationNo(), bank,
                                    tollTag.getTollRegistration())) {
                                  LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                                      .message("Failed to send email to whitelisting of tag.")
                                      .data("user email",
                                          tollTag.getTollRegistration().getEmailId())
                                      .format());
                                }

                              }
                              else if (tollTag.getExcCode().equalsIgnoreCase(TollConstant.EXC_CODE_CLOSED_OR_REPLACED)
                                    && operation.equalsIgnoreCase(TollConstant.ADD_OP)) {

                                // success sms
//                                if (!applicationProperties.getAwsSMSClient()
//                                        && !smsClient.sendSms(tollTag.getTollRegistration().getMobileNo(),
//                                                Templates.TollTagClosingOrReplacing.format(bank.getAlias()), true)) {
//                                    LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
//                                            .message("Failed to send SMS")
//                                            .data("phone", tollTag.getTollRegistration().getMobileNo()).format());
//                                }
//
//                                if (applicationProperties.getAwsSMSClient()) {
//
//                                    NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
//                                    notificationRequestContext.setTraceId(httpServletContext.getTraceId());
//                                    if (!notificationResource.sendSms(tollTag.getTollRegistration().getMobileNo(),
//                                            Templates.TollTagClosingOrReplacing.format(bank.getAlias()),
//                                            httpServletContext.getClientFeatureFlags().smsEnabled(),
//                                            notificationRequestContext)) {
//
//                                        LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
//                                                .message("Failed to send SMS")
//                                                .data("phone", tollTag.getTollRegistration().getMobileNo())
//                                                .format());
//
//                                    }
//                                }

                                // Email
//                                if (!this.tollEmailUtils.tollTagClosingOrReplacing(tollTag.getRegistrationNo(), bank,
//                                        tollTag.getTollRegistration())) {
//                                    LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
//                                            .message("Failed to send email to closing or replacing of tag.")
//                                            .data("user email", tollTag.getTollRegistration().getEmailId())
//                                            .format());
//                                }
                                  //commenting for uat testing

                                if (!applicationProperties.getAwsSMSClient()
                                        && !smsClient.sendSms(tollTag.getTollRegistration().getMobileNo(),
                                                Templates.TollTagClosingOrReplacing.format(bank.getAlias()), true)) {
                                    LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                                            .message("Failed to send SMS")
                                            .data("phone", tollTag.getTollRegistration().getMobileNo()).format());
                                }

                                if (applicationProperties.getAwsSMSClient()) {

                                    NotificationRequestContext notificationRequestContext = new NotificationRequestContext();
                                    notificationRequestContext.setTraceId(httpServletContext.getTraceId());
                                    if (!notificationResource.sendSms(tollTag.getTollRegistration().getMobileNo(),
                                            Templates.TollTagClosingOrReplacing.format(bank.getAlias()),
                                            httpServletContext.getClientFeatureFlags().smsEnabled(),
                                            notificationRequestContext)) {

                                        LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                                                .message("Failed to send SMS")
                                                .data("phone", tollTag.getTollRegistration().getMobileNo())
                                                .format());

                                    }
                                }

                                // Email
                                //commenting for uat testing
                                if (!this.tollEmailUtils.tollTagClosingOrReplacing(tollTag.getRegistrationNo(), bank,
                                        tollTag.getTollRegistration())) {
                                    LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
                                            .message("Failed to send email to closing or replacing of tag.")
                                            .data("user email", tollTag.getTollRegistration().getEmailId())
                                            .format());
                                }

								}
								
							} catch (Exception e) {
								LOG.error(LogFormatter.instance(httpServletContext.getTraceId())
										.message("Exception in sending email & sms to Customer.").format());
							}

						}

					}

					if (CollectionUtils.isEmpty(tags)) {
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Tags list empty so continuing to next bank").format());
						continue;
					}

					tagList.setTag(tags);
					txn.setTagList(tagList);

					tollMngTagExceptionRequest.setHead(head);
					tollMngTagExceptionRequest.setTxn(txn);

					// Create JAXB Context
					JAXBContext jaxbContext = JAXBContext.newInstance(TollMngTagExceptionRequest.class);

					// Create Marshaller
					Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

					// Required formatting??
					jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

					// Print XML String to Console
					StringWriter sw = new StringWriter();

					// Write XML to StringWriter
					jaxbMarshaller.marshal(tollMngTagExceptionRequest, sw);

					// Verify XML Content
					String postData = sw.toString();

					ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
							postData.getBytes(StandardCharsets.UTF_8));

					ByteArrayOutputStream byteArrayOutputStream = tollSignatureGenerationServices
							.signatureGenerationServices(byteArrayInputStream, httpServletContext.getTraceId(),
									bank.getOrgId());
					String responseData = null;
					
					// NPCI Active Active Setup Phase2 changes
					String ipAddress = this.tollHttpsServices.npciHealthCheckApi(httpServletContext.getTraceId());
					if (StringUtils.isEmpty(ipAddress)) {

						return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
								.body(APIResponse.error(HeaderCode.NETC_NPCI_SERVER_DOWN));

					}
					int port = this.tollProperties.getNetcHealthCheckPort();
					String endPoint = this.tollProperties.getManageExceptionUrl();
					String url = "https://" + ipAddress + ":" + port + endPoint;
				      
					try {
						TollHttpsServices.bankIin = bank.getIin();
						
						/*
						 * responseData = tollHttpsServices.send(tollProperties.getManageExceptionUrl(),
						 * httpServletContext.getTraceId(), byteArrayOutputStream);
						 */
						
						responseData = tollHttpsServices.send(url,
								httpServletContext.getTraceId(), byteArrayOutputStream);
						
						TollHttpsServices.bankIin = StringUtils.EMPTY;
						if (responseData == null) {
							LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
									.message("NPCI response empty so continuing to next bank").format());
							continue;
						}
					} catch (Exception e) {
						e.printStackTrace();
						LOG.debug(LogFormatter.instance(httpServletContext.getTraceId())
								.message("Exception Occured so continuing to next bank").format());
						continue;
					}

					tollMngTagExceptionResponse = JAXB.unmarshal(new StringReader(responseData),
							TollMngTagExceptionResponse.class);

					formatter = new SimpleDateFormat(TollConstant.TS_DATE_FORMAT);
					TollMngTagExceptionResponse tollMngTagExceptionResp = objectMapper.readValue(
							objectMapper.writeValueAsString(tollMngTagExceptionResponse),
							TollMngTagExceptionResponse.class);
					tollMngTagExceptionResp.getTxn().getResp().getTag().clear();

					if (operation.equals(TollConstant.ADD_OP) || operation.equals(TollConstant.REMOVE_OP)) {
						for (Tag tag : tollMngTagExceptionResponse.getTxn().getResp().getTag()) {
							for (TollTag tollTag : tollTags) {
								if (tollTag.getTagId().equalsIgnoreCase(tag.getTagId())) {

									tollMngTagExceptionResp.getTxn().getResp().setTag(Arrays.asList(
											objectMapper.readValue(objectMapper.writeValueAsString(tag), Tag.class)));
									tollTag.setUpdateExceptionResponse(
											objectMapper.writeValueAsString(tollMngTagExceptionResp));
									tollTag.setUpdateExceptionErrorCode(tag.getErrCode());
									tollTag.setExcCodeUpdateTime(String.valueOf(
											formatter.parse(tollMngTagExceptionResp.getTxn().getTs()).getTime()));
									tollTag.setExcCode(operation.equalsIgnoreCase(TollConstant.REMOVE_OP)
											? TollConstant.SUCCESS_RESPONSE
											: tollTag.getExcCode());
									tollDBService.updateTollTag(tollTag);
								}
							}
						}
					}
				}
				tollMngTagExceptionResponseList.add(tollMngTagExceptionResponse);
			}

			return ResponseEntity.status(HttpStatus.OK).body(tollMngTagExceptionResponseList);
		}

		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(APIResponse.error(HeaderCode.TOLL_USER_DETAILS_CANNOT_UPDATE));
	}
	
	 // From rabbitMq this API is called for further debit process
    public void fastagDebitJob(List<String> partnerTransactionId, String traceId) throws Exception {
		try {
			if (partnerTransactionId == null) {
				LOG.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Request is null").format());
			}
			LOG.debug(LogFormatter.instance(traceId).data("partnerTransactionId", partnerTransactionId).format());

			// fetching dipcoinTransaction using partnerTransactionIds
			List<DipcoinTransaction> dipcoinTransaction = coinDBService
					.findDipcoinTransactionByPartnerTransactionReferenceId(partnerTransactionId);

			LOG.debug(LogFormatter.instance(traceId).data("Fetched dipcoinTransaction by :", dipcoinTransaction)
					.format());

			if (dipcoinTransaction.isEmpty()) {
				LOG.info(LogFormatter.instance().message("Empty List of DipcoinTransaction").format());
			}

			List<String> partnerTransactionReferenceIdList = new ArrayList<>();
			List<Integer> dtxIds = new ArrayList<>();

			for (DipcoinTransaction dtxns : dipcoinTransaction) {
				String partnerTransactionReferenceId = dtxns.getPartnerTransactionReferenceId();
				LOG.debug(LogFormatter.instance(traceId)
						.data("partnerTransactionReferenceId", partnerTransactionReferenceId).format());

				// If dtx type is 19
				if (dtxns.getType() == DBConstants.DipcoinTransactionType.DEEMED_ACCEPTED.value()) {
					if (dtxns.getCustomerAccountId() == 0) {
						LOG.debug(LogFormatter.instance(traceId)
								.data("Customer Account Id null for :", partnerTransactionReferenceId).format());
						continue;
					} else {
						int customerAccountId = dtxns.getCustomerAccountId();
						String usageDetails = dtxns.getUsageDetails();
				        String vehicleNo = usageDetails.split("[ ,]")[0];
//						String vehicleNo = dtxns.getUsageDetails().substring(0, dtxns.getUsageDetails().indexOf(' '));
						LOG.debug(LogFormatter.instance(traceId).data("vehicleNo :", vehicleNo).format());
						// by using vehicleNo checking if the vehicle is in low balance or not
						List<TollTag> tollTags = tollDBService.findTollTagByRegistrationNoAndStatus(vehicleNo,
								Arrays.asList(String.valueOf(DBConstants.TollTagApprovalStatus.ACTIVE.value())));
						TollTag tollTag = tollTags.get(0);
						if (tollTag != null) {
							String excCode = tollTag.getExcCode();
							if (DBConstants.TollTagExcCodeStatus.ACTIVE.value().equals(excCode)) {
								LOG.debug(LogFormatter.instance(traceId).data(
										"Vehicle is active calling processDipcoinTransaction for type 19 for partnerTransactionRefId :",
										partnerTransactionReferenceId).format());
								// this method is common method to update the transactions according to the
								// requirements
								this.processDipcoinTransaction(dtxIds,dtxns, customerAccountId, partnerTransactionReferenceId,
										traceId);

							} else if (DBConstants.TollTagExcCodeStatus.LOW_BALANCE.value().equals(excCode)) {
								LOG.debug(LogFormatter.instance(traceId)
										.data("Vehicle in low balance for type 19  :", partnerTransactionReferenceId)
										.format());
								continue;
							}
						} else {
							LOG.debug(LogFormatter.instance(traceId)
									.data("TollTag is Empty for  :", partnerTransactionReferenceId).format());
							continue;
						}
					}
				}

				// if type is 2,3,4
				if (dtxns.getType() == DBConstants.DipcoinTransactionType.VALIDATION_FAILURE.value()
						|| dtxns.getType() == DBConstants.DipcoinTransactionType.PARTIALLY_USED.value()
						|| dtxns.getType() == DBConstants.DipcoinTransactionType.COMPLETELY_USED.value()) {

					// checks in btx if already debited or not
					BankTransaction btx = this.bankDBService.getBankTransactionByDipcoinIdAndAmountAndTypeAndStatus(
							dtxns.getDipcoinId(), dtxns.getAmount(),
							DBConstants.BankTransactionType.DEBIT_TO_ACCOUNT.value(),
							DBConstants.BankTransactionsStatus.SUCCESS.value());
					LOG.debug(LogFormatter.instance(traceId).data("btx", btx)
							.data("partnerTransactionReferenceId", partnerTransactionReferenceId).format());

					if (btx != null) {
						LOG.info(LogFormatter.instance().message("Type 6 entry found already debited")
								.data("for partnerTransactionReferenceId", partnerTransactionReferenceId).format());
						continue;
					} else {
						if (dtxns.getCustomerAccountId() == 0) {
							LOG.info(LogFormatter.instance(httpServletContext.getTraceId())
									.message("CustomerAccountId is Null")
									.data("for partnerTransactionReferenceId", partnerTransactionReferenceId).format());
							continue;
						} else {
							int customerAccountId = dtxns.getCustomerAccountId();
							LOG.debug(LogFormatter.instance(traceId).data("customerAccountId", customerAccountId)
									.data("for partnerTransactionReferenceId", partnerTransactionReferenceId).format());

							// this method is common method to update the transactions according to the
							// requirements
							this.processDipcoinTransaction(dtxIds,dtxns, customerAccountId, partnerTransactionReferenceId,
									traceId);

						}

					}
				}

				// if type is 15
				if (dtxns.getType() == DBConstants.DipcoinTransactionType.CHARGE_BACK.value()) {
					LOG.debug(LogFormatter.instance(traceId)
							.data("Given transaction is Type 15 i.e ChargeBack for partnerTransactionReferenceId:",
									partnerTransactionReferenceId)
							.format());

					continue;
				}

				// if type is 21
				if (dtxns.getType() == DBConstants.DipcoinTransactionType.CLONE_TRANSACTION.value()) {
					LOG.debug(LogFormatter.instance(traceId).data(
							"Given transaction is Type 19 i.e CloneTransaction for partnerTransactionReferenceId:",
							partnerTransactionReferenceId).format());
					continue;
				}

			}
			if (!dtxIds.isEmpty()) {
                offlineJobClient.initTollDeemedTransactionAccountingJob(dtxIds, traceId);
                LOG.info(LogFormatter.instance(traceId).message("Job Run Successfully").format());
            }
		} catch (Exception e) {
			LOG.error(LogFormatter.instance(traceId).message("Error in fastagDebitJob method").format(), e);
			throw e;
		}
	}
    
    public void processDipcoinTransaction(List<Integer> dtxIds,DipcoinTransaction dtxns, int customerAccountId,
			String partnerTransactionReferenceId, String traceId) throws Exception {
		try {
			// in this query it checks for the Active dipcoin
			Dipcoin dipcoin = coinDBService.findDipcoinByCustomerAccountIdAndUsageTypeAndStatusAndStatusWithBank(
					customerAccountId, DBConstants.DipcoinUsageType.TOLL.value(),
					DBConstants.DipcoinStatus.ACTIVE.value(), 0);
			LOG.debug(LogFormatter.instance(traceId).data("Checking Active DIpcoin for :", dipcoin)
					.data("for partnerTransactionReferenceId", partnerTransactionReferenceId).format());

			if(dipcoin != null) {
			// Convert expiry time to Date
			Date expiryDate = new Date(Long.valueOf(dipcoin.getExpiryTime()));
			// Get current time
			Date currentDate = new Date();
			// Check if the expiry time is less than the current time
			boolean isExpired = expiryDate.before(currentDate);
			

			// first it checks if the dipcoin is expired if (expired) --> then it cancles
			// the active dipcoin and update dtx to type 19 and try to debit
			if (isExpired) {
				LOG.debug(LogFormatter.instance(traceId).data("isExpired", isExpired)
						.data("for partnerTransactionReferenceId", partnerTransactionReferenceId).format());
				dipcoin.setStatus(DBConstants.DipcoinStatus.CANCELLED.value());
				coinDBService.updateCoin(dipcoin);
				dtxns.setType(DBConstants.DipcoinTransactionType.DEEMED_ACCEPTED.value());
				this.coinDBService.updateTransaction(dtxns);
				LOG.debug(LogFormatter.instance(traceId)
						.data("Calling tollDeemed Job for partnerTransactionReferenceId", partnerTransactionReferenceId)
						.format());
				dtxIds.add(dtxns.getId());
//				offlineJobClient.initTollDeemedTransactionAccountingJob(dtxns.getId(),dtxns.getRequestTime(), traceId);
				LOG.debug(LogFormatter.instance(traceId)
						.data("succefully called job for expired dipcoin for partnerTransactionReferenceId",
								partnerTransactionReferenceId)
						.format());
				return;
			}
			}

//			//if we dont active , search for cancelled 
//			if (dipcoin == null) {
//				dipcoin = coinDBService.findDipcoinByCustomerAccountIdAndUsageTypeAndStatusAndStatusWithBank(
//						customerAccountId, DBConstants.DipcoinUsageType.TOLL.value(),
//						DBConstants.DipcoinStatus.CANCELLED.value(), 0);
			if (dipcoin == null) {
				dtxns.setType(DBConstants.DipcoinTransactionType.DEEMED_ACCEPTED.value());
				this.coinDBService.updateTransaction(dtxns);
				LOG.debug(LogFormatter.instance(traceId)
						.data("Calling tollDeemed Job for not getting active dipcoin for partnerTransactionReferenceId",
								partnerTransactionReferenceId)
						.format());
				dtxIds.add(dtxns.getId());
//				offlineJobClient.initTollDeemedTransactionAccountingJob(dtxns.getId(),dtxns.getRequestTime(), traceId);
				LOG.info(LogFormatter.instance().message("Successfully ran job").format());
				return;
			}

			Integer activeDipcoinId = dipcoin.getId();
			// if we get the Active dipcoin then check if the lien is there or not on that
			// dipcoin
			if (activeDipcoinId != 0) {
				BankTransaction btx = bankDBService.getBankTransactionByDipcoinIdAndTypeAndStatus(activeDipcoinId,
						DBConstants.BankTransactionType.LIEN_MARK.value(),
						DBConstants.BankTransactionsStatus.SUCCESS.value());
				LOG.debug(LogFormatter.instance(traceId).data("Btx Active dipcoin", btx)
						.data("for partnerTransactionReferenceId", partnerTransactionReferenceId).format());

				if (btx != null) {
					// if lien is present then update dtx entry to type 19 and call job
					dtxns.setType(DBConstants.DipcoinTransactionType.DEEMED_ACCEPTED.value());
					this.coinDBService.updateTransaction(dtxns);
					LOG.debug(LogFormatter.instance(traceId).data("dtxns convert 19 and calling job", dtxns)
							.data("for partnerTransactionReferenceId", partnerTransactionReferenceId).format());
					dtxIds.add(dtxns.getId());
//					offlineJobClient.initTollDeemedTransactionAccountingJob(dtxns.getId(),dtxns.getRequestTime(), traceId);
					LOG.info(LogFormatter.instance().message("Successfully ran job when BTX is not null").format());
					return;

				} else {
					// if lien is not present then update active dipcoin to cancelled and then
					// update dtx entry to type 19 and call job
					Dipcoin activeDipcoin = coinDBService.getDipcoinById(activeDipcoinId);
					activeDipcoin.setStatus(DBConstants.DipcoinStatus.CANCELLED.value());
					if (coinDBService.updateCoin(activeDipcoin) != null) {
						dtxns.setType(DBConstants.DipcoinTransactionType.DEEMED_ACCEPTED.value());
						if (this.coinDBService.updateTransaction(dtxns) != null) {
							LOG.debug(LogFormatter.instance(traceId).data(
									"Calling tollDeemed Job after cancelling the active dipcoin for partnerTransactionReferenceId",
									partnerTransactionReferenceId).format());
							dtxIds.add(dtxns.getId());
							LOG.info(LogFormatter.instance().message("Successfully ran job when BTX is null").format());
							return;
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.error(LogFormatter.instance(traceId).message("Error in  processDipcoinTransaction method").format(), e);
			throw e;

		}

	}
    

}
