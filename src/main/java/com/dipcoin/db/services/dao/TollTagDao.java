package com.dipcoin.db.services.dao;


import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.dipcoin.db.services.model.TollRegistration;
import com.dipcoin.db.services.model.TollTag;

@Repository
public interface TollTagDao extends JpaRepository<TollTag, Integer> {

  public TollTag findByTagId(final String tagId);

  public List<TollTag> findByBankIdAndStatusOrderByIdDesc(final Integer bankId,
      final String status);
  
  public List<TollTag> findByWalletBankIdAndStatusOrderByIdDesc(final Integer bankId,
	      final String status);

  public List<TollTag> findTollTagByStatusOrderByBankId(final String status);

  public List<TollTag> findTollTagByRegistrationNoIn(final List<String> registrationNo);

  public List<TollTag> findByCustomerAccountIdIn(final List<Integer> customerAccountId);
  
  public List<TollTag> findDistinctByBankIdAndCreatedDateTimeBetweenOrderByIdDesc(final Integer bankId, final String starTime, final String endTime, Pageable pageable);
  
  public List<TollTag> findDistinctByBankIdAndStatusAndCreatedDateTimeBetweenOrderByIdDesc(final Integer bankId,final String Status, final String starTime, final String endTime, Pageable pageable);
  
  public List<TollTag> findDistinctByWalletBankIdAndStatusAndCreatedDateTimeBetweenOrderByIdDesc(final Integer bankId,final String Status, final String starTime, final String endTime, Pageable pageable);
  
  public List<TollTag> findDistinctByBankIdAndRegistrationNoAndCreatedDateTimeBetweenOrderByIdDesc(final Integer bankId,final String VehicleNumber, final String starTime, final String endTime, Pageable pageable);

  public List<TollTag> findDistinctByWalletBankIdAndRegistrationNoAndCreatedDateTimeBetweenOrderByIdDesc(final Integer bankId,final String VehicleNumber, final String starTime, final String endTime, Pageable pageable);
  
  public List<TollTag> findDistinctByBankIdAndCreatedDateTimeBetweenOrderByIdDesc(final Integer bankId, final String starTime, final String endTime);

  public List<TollTag> findByBankIdAndStatusInAndVendorUpdateDateTimeBetweenOrderByIdDesc(final Integer bankId,final List<String> status, final String starTime, final String endTime, Pageable pageable);

  public List<TollTag> findByBankIdAndApprovedDateTimeBetweenOrderByIdDesc(final Integer bankId, final String starTime, final String endTime);
  
  public List<TollTag> findByBankIdAndCreatedDateTimeBetweenOrderByIdDesc(final Integer bankId, final String starTime, final String endTime);
  
  public List<TollTag> findByBankIdAndExcCodeUpdateTimeBetweenOrderByIdDesc(final Integer bankId, final String starTime, final String endTime);

  public TollTag findTollTagByCustomerAccountIdAndTagIdAndRegistrationNo(Integer customerAccountId,
      String tollTagId, String registrationNo);
  
  @Query("SELECT SUM(t.minimumAmount) FROM TollTag t where t.customerAccountId =:customerAccountId AND t.status IN(:statuses)")
  public BigDecimal findByCustomerAccountId_sumOfMinimumAmount(@Param("customerAccountId") Integer customerAccountId, @Param("statuses") List<String> statuses);
  
  public Long countByBankIdAndStatusIn(Integer bankId, List<String> statuses);
  
  public Long countByBankIdAndDispatchDateBetweenOrderByIdDesc(final Integer bankId, final String startTime, final String endTime);
  
  public Long countByBankIdAndDeliveryDateBetweenOrderByIdDesc(final Integer bankId, final String startTime, final String endTime);
  
  public List<TollTag> findTollTagByRegistrationNoAndBankId(final String registrationNo, final Integer bankId);

  public TollTag findTollTagByTagIdOrTid(final String tagId, final String tid);
  
  public List<TollTag> findTollTagByRegistrationNoAndStatusIn(final String registrationNo, final List<String> status);

  public List<TollTag> findTollTagByBankIdAndStatusInAndApprovedDateTimeBetweenOrderByIdDesc(final Integer bankId,
      final List<String> status,
      final String starTime, final String endTime);  
  
  public List<TollTag> findTollTagByExcCodeAndStatus(String excCode, String Status, Pageable pageable);
  
  public List<TollTag> findTollTagByBankIdAndStatusInAndSerialNumberAndApprovedDateTimeBetweenOrderByIdDesc(
      Integer bankId, List<String> status, String serialNumber, String starTime, String endTime);

  public List<TollTag> findTollTagByBankIdAndStatusInAndRegistrationNoAndApprovedDateTimeBetweenOrderByIdDesc(
      Integer bankId, List<String> status, String vehiclenumber, String starTime, String endTime);
  
  public List<TollTag> findTollTagByCustomerAccountIdInAndStatusIn(
      final List<Integer> customerAccountId, final List<String> status);
  
  public Long countByCreatedDateTimeLessThan(final String createdDateTime);
  
  public Long countByTagIdNotNullAndCreatedDateTimeLessThan(final String createdDateTime);
  
  public TollTag findTollTagBySerialNumber(String serialNumber);

  public List<TollTag> findTollTagsBySerialNumberOrderByIdDesc(String serialNumber);

  public TollTag findTollTagBySerialNumberAndBankId(String serialNumber, int bankId);
  
  public List<TollTag> findTollTagsByAccountNumberAndStatus(String accountNumber,String Status); 

  public List<TollTag> findTollTagByStatusAndCreatedDateTimeLessThan(final String status, final String createdDateTime, Pageable pageable);

  public List<TollTag> findTollTagsByAccountNumber(String accountNumber);

  public List<TollTag> findTollTagByStatusAndCreatedDateTimeLessThan(final String status, final String createdDateTime);
  
  public List<TollTag> findTollTagByBankIdAndTollRegistrationDobAndApprovedDateTimeBetweenAndStatusIn(Integer bankId,String branchCode,String startTime,String endTime,
		  List<String> status);
  
  public List<TollTag> findTollTagByBankIdAndTollRegistrationCreatedByAndApprovedDateTimeBetweenAndStatusIn(Integer bankId,Integer createdBy,
		  String startTime,String endTime,List<String> status);
  
	@Query("SELECT t FROM TollTag t WHERE t.bankId = :bankId AND t.tollRegistration IN :tollRegistrations AND t.status = :status ORDER BY t.id DESC")
	public List<TollTag> findTollTagByBankIdAndTollRegistrationAndStatusOrderByIdDesc(@Param("bankId") Integer bankId,
			@Param("tollRegistrations") List<TollRegistration> tollRegistrations, @Param("status") String status);

  public List<TollTag> findTollTagByBankIdAndTollRegistrationCreatedByAndStatusOrderByIdDesc(final Integer bankId,
			final Integer createdBy, final String status);
  
  public List<TollTag> findDistinctByMerchantIdAndRegistrationNoOrderByIdDesc(final Integer merchantId,final String VehicleNumber, Pageable pageable);
  
  public List<TollTag> findDistinctByMerchantIdAndCreatedDateTimeBetweenOrderByIdDesc(final Integer merchantId, final String starTime, final String endTime, Pageable pageable);
  
  public Long countByMerchantIdAndStatusIn(Integer merchantId, List<String> statuses);
  
  @Query("SELECT SUM(t.registrationAmount) FROM TollTag t where t.merchantId = :merchantId AND "
			+ "t.merchantAmountPaid IN(:merchantAmountPaid)")
  public BigDecimal findByMerchantId_sumOfRegistrationAmount(@Param("merchantId") Integer merchantId,
			@Param("merchantAmountPaid") List<Integer> merchantAmountPaid);

  @Query("SELECT SUM(t.minimumAmount) FROM TollTag t where t.merchantId = :merchantId AND "
			+ "t.merchantAmountPaid IN(:merchantAmountPaid)")
  public BigDecimal findByMerchantId_sumOfMinimumAmount(@Param("merchantId") Integer merchantId,
			@Param("merchantAmountPaid") List<Integer> merchantAmountPaid);

  public Long countByMerchantIdAndMerchantAmountPaidIn(Integer merchantId, List<Integer> merchantAmountPaid);
  
  @Query("SELECT SUM(t.registrationAmount) FROM TollTag t where t.merchantAmountPaid = :merchantAmountPaid AND "
			+ "t.merchantId IN(:merchantId) AND t.walletBankId = :walletBankId AND t.merchantAmountSettled <> :merchantAmountSettled")
  public BigDecimal sumOfMerchantRegistrationAmount(@Param("merchantId") List<Integer> merchantId, @Param("walletBankId") int walletBankId, @Param("merchantAmountPaid") int merchantAmountPaid, @Param("merchantAmountSettled") int merchantAmountSettled);
  
  @Query("SELECT SUM(t.minimumAmount) FROM TollTag t where t.merchantAmountPaid = :merchantAmountPaid AND "
			+ "t.merchantId IN(:merchantId) AND t.walletBankId = :walletBankId AND t.merchantAmountSettled <> :merchantAmountSettled")
  public BigDecimal sumOfMerchantMinimumAmount(@Param("merchantId") List<Integer> merchantId, @Param("walletBankId") int walletBankId, @Param("merchantAmountPaid") int merchantAmountPaid, @Param("merchantAmountSettled") int merchantAmountSettled);
  
  public List<TollTag> findTollTagByMerchantIdInAndWalletBankIdAndMerchantAmountPaidAndMerchantAmountSettledNot(List<Integer> merchantIds, int walletBankId, int merchantAmountPaid, int merchantAmountSettled);

  public List<TollTag> findTollTagByMerchantIdAndMerchantAmountPaidIn(Integer merchantId, List<Integer> merchantAmountPaid);

  public List<TollTag> findTollTagByVinVrnFlagAndStatusAndVinDocVerifyIn(final Integer vinVrnFlag, final String status,  final List<Integer> vinDocVerify);
 
  public List<TollTag> findByBankIdAndVinVrnFlagAndStatusAndApprovedDateTimeBetweenAndTollRegistrationDob(Integer bankId, Integer vinVrnFlag, String Status, 
		  String starTime, final String endTime, String branchCode ,Pageable pageable);
  
  public TollTag findByBankIdAndVinNumberAndVinToVrnAndSerialNumber(Integer bankId,String vin, String vrn, String serialNumber);

  public List<TollTag> findByVinNumberAndSerialNumberAndStatusAndVinVrnFlag(String vinNumber,String serialNumber, String status, int vinVrnFlag);
  
  public List<TollTag> findDistinctByBankIdAndAccountNumberAndCreatedDateTimeBetweenOrderByIdDesc(final Integer bankId,final String accountNumber, final String starTime, final String endTime, Pageable pageable);

  public List<TollTag> findDistinctByWalletBankIdAndAccountNumberAndCreatedDateTimeBetweenOrderByIdDesc(final Integer bankId,final String accountNumber, final String starTime, final String endTime, Pageable pageable);

 public List<TollTag> findDistinctByBankIdAndSerialNumberAndCreatedDateTimeBetweenOrderByIdDesc(final Integer bankId,final String serialNumber, final String starTime, final String endTime, Pageable pageable);

  public List<TollTag> findDistinctByWalletBankIdAndSerialNumberAndCreatedDateTimeBetweenOrderByIdDesc(final Integer bankId,final String serialNumber, final String starTime, final String endTime, Pageable pageable);

  public List<TollTag> findDistinctByBankIdAndCustomerAccountIdInAndCreatedDateTimeBetweenOrderByIdDesc(final Integer bankId,final List<Integer> CustomerAccountId, final String starTime, final String endTime, Pageable pageable);

  public List<TollTag> findDistinctByWalletBankIdAndCustomerAccountIdInAndCreatedDateTimeBetweenOrderByIdDesc(final Integer bankId,final  List<Integer> CustomerAccountId, final String starTime, final String endTime, Pageable pageable);

public List<TollTag> findDistinctByBankIdAndTagIdAndCreatedDateTimeBetweenOrderByIdDesc(final Integer bankId,final String epcRfIdTag, final String starTime, final String endTime, Pageable pageable);

  public List<TollTag> findDistinctByWalletBankIdAndTagIdAndCreatedDateTimeBetweenOrderByIdDesc(final Integer bankId,final String epcRfIdTag, final String starTime, final String endTime, Pageable pageable);
  
  public List<TollTag> findTollTagByVinNumber(String vinNumber);
  
  public List<TollTag> findTollTagByEngineNo(String engineNo);

  public List<TollTag> findTollTagByBankIdAndTollRegistrationDobAndStatusOrderByIdDesc(final Integer bankId,
			final String dob, final String status);

  public List<TollTag> findByBankIdAndVinVrnFlagAndStatusAndApprovedDateTimeBetweenAndSerialNumber(final Integer bankId,final Integer vinVrnFlag,final String status,String startTime,String endTime,String serialNumber, Pageable pageable);
  
  public List<TollTag> findTollTagByRegistrationNoAndSerialNumberAndStatus(final String registrationNo,final String serialNumber, final String status);

  public TollTag findTollTagByTagId(final String tagId); 
  
  public List<TollTag> findTollTagByTollRegistrationId(Integer tollRegistrationId );
  

  public List<TollTag> findTollTagByIsForceClose(final Integer IsForceClose);
  
  public List<TollTag> findTollTagByIsForceCloseAndForceClosedConsentAndStatus(final Integer isForceClose , final Integer forceClosedConsent , final String status);
  
  public TollTag findTollTagByRegistrationNo(final String registrationNo);
  
}
 
 


 

