package com.dipcoin.api.model;

import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.commons.CoreUtils;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.services.commons.DBConstants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class UserVerifyRequest extends APIRequest {
  private static final Logger LOG = LogManager.getLogger(UserVerifyRequest.class);

  @ApiModelProperty(required = false)
  private String phonenum;
  @ApiModelProperty(required = false)
  private String otp;
  @ApiModelProperty(required = false)
  private String code;
  @ApiModelProperty(required = false)
  private String password;
  @ApiModelProperty(required = false)
  private String pin;
  @ApiModelProperty(readOnly = false)
  private Integer tnc;
  
  @ApiModelProperty(required = false)
  private String tpin;
  
  @ApiModelProperty(required = false)
  private String cardId;
  
  @ApiModelProperty(required = false)
  private String source;
  
  @JsonIgnore
  @Setter(value = AccessLevel.PRIVATE)
  private String email;
  @JsonIgnore
  @Setter(value = AccessLevel.PRIVATE)
  private String emailHash;
  @JsonIgnore
  @Setter(value = AccessLevel.PRIVATE)
  private Boolean accepted = true;
  @JsonIgnore
  @Setter(value = AccessLevel.PRIVATE)
  private DateTime expiryDate;

  @ApiModelProperty(required = false)
  private String authorizationPin;
  
  @JsonIgnore
  private static String ACCEPT = "ACCEPT";

  private static DateTime dateTime = DateTime.now(DateTimeZone.UTC);

  // only getter
  public final String getEmail() {
    return email;
  }

  public final boolean isAccepted() {
    return accepted;
  }

  public final String getEmailHash() {
    return emailHash;
  }

  public final DateTime getExpiryDate() {
    return expiryDate;
  }

  @Override
  public boolean validate(HttpServletContext httpServletContext) {
    if (!isDecrypted()) {
      decrypt(httpServletContext);
      if (!isDecrypted())
        return false;
    }

    if (getPhonenum().length() != DBConstants.MOBILE_LENGTH
        || !NumberUtils.isCreatable(getPhonenum())) {
      this.setErrorCode(HeaderCode.USER_INVALID_PHONENUM);
      return false;
    }

    // @NOTE - do not check for empty as NumberUtils.isCreatable will fail for the same
    if (getOtp() != null && !NumberUtils.isCreatable(getOtp())) {
      this.setErrorCode(HeaderCode.USER_INVALID_OTP);
      return false;
    }

    DateTime expiryDate = getExpiryDate();
    if (expiryDate != null && dateTime.isAfter(expiryDate))
      return false;

    boolean emailValid = false, phoneValid = false;
//    if (getEmail() != null && getEmailHash() != null
//        && EmailValidator.getInstance().isValid(getEmail()) && !getEmailHash().isEmpty()) {
//      emailValid = true;
//    } else {
//      this.email = null;
//      this.emailHash = null;
//    }

    if (getPhonenum() != null && getOtp() != null && CoreUtils.validPhoneNumber(getPhonenum())
        && getOtp().length() == DBConstants.OTP_LENGHT) {
      phoneValid = true;
    } else {
      this.phonenum = null;
      this.otp = null;
    }

    return emailValid || phoneValid;
  }

  @Override
  protected void decrypt(HttpServletContext httpServletContext) {
    setDecrypted(true);

    if (this.code != null) {
      try {
        // code =
        // ENCRYPTED(email-emailhash-phonenum-exiprytime-ACCEPT/REJECT)
        // The encryption part is in UserLoginResource.java
        this.code = new String(
            this.getCryptoUtil().decrypt(Base64.decodeBase64(this.code), this.getEncryptionAlgo()),
            StandardCharsets.UTF_8);

        StringTokenizer tokenizer = new StringTokenizer(this.code, "-");
        if (tokenizer.countTokens() == 5) {
          this.email = tokenizer.nextToken();
          this.emailHash = tokenizer.nextToken();

          String num = tokenizer.nextToken(); // phonenum can be
                                              // explicitly pass in
                                              // request too. if not,
                                              // then get it from code
          if (this.phonenum == null)
            this.phonenum = num;

          this.expiryDate = new DateTime(Long.valueOf(tokenizer.nextToken()));

          this.accepted = (ACCEPT.equals(tokenizer.nextToken())) ? true : false;
        }
      } catch (Exception e) {
        LOG.error(
            LogFormatter.instance(httpServletContext.getTraceId()).message("Exception").format(),
            e);
        setDecrypted(false);
      }
    }
  }

  public boolean phoneOnlyValidation() {
    if (StringUtils.isEmpty(this.email) && StringUtils.isEmpty(this.code)
        && !StringUtils.isEmpty(this.phonenum) && !StringUtils.isEmpty(this.otp)) {
      return true;
    }
    return false;
  }

  public boolean emailOnlyValidation() {
    if (!StringUtils.isEmpty(this.email) && !StringUtils.isEmpty(this.code)
        && StringUtils.isEmpty(this.phonenum) && StringUtils.isEmpty(this.otp)) {
      return true;
    }
    return false;
  }
  
  public boolean validateEmail(HttpServletContext httpServletContext) {
    if (!isDecrypted()) {
      decrypt(httpServletContext);
      if (!isDecrypted()) {
        return false;
      }
      if (StringUtils.isEmpty(this.email) && StringUtils.isEmpty(this.phonenum)
          && StringUtils.isEmpty(this.emailHash)) {
        return false;
      }
    }
    return true;
  }
}
