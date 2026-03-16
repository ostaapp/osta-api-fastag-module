package com.dipcoin.api.filter.request;

import com.dipcoin.api.commons.APIConstants;
import com.dipcoin.api.commons.APIConstants.RequestType;
import com.dipcoin.api.commons.HeaderCode;
import com.dipcoin.api.filter.HttpServletContext;
import com.dipcoin.api.model.APIResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.dipcoin.client.UserServiceClient;
//import com.dipcoin.api.resource.UserLoginResource;
//import com.dipcoin.api.resource.UserLoginSession;
import com.dipcoin.commons.LogFormatter;
import com.dipcoin.db.services.BankDBService;
import com.dipcoin.db.services.MerchantDBService;
import com.dipcoin.db.services.UserDBService;
import com.dipcoin.db.services.model.Bank;
//import com.dipcoin.db.services.model.CustomerAccount;
import com.dipcoin.db.services.model.Merchant;
import com.dipcoin.db.services.model.User;
//import com.dipcoin.api.config.ApplicationProperties;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

//import com.dipcoin.api.resource.UserLoginSession;
@Slf4j
@Component("com.dipcoin.api.filter.request.AuthorizationInterceptor")
public class AuthorizationInterceptor implements RequestInterceptor {
  private static final Logger log = LogManager.getLogger(AuthorizationInterceptor.class);
  //
  // @Autowired
  // private UserLoginSession userLoginSession;
  @Autowired
  private UserDBService userDBService;
  @Autowired
  private MerchantDBService merchantDBService;
  @Autowired
  private BankDBService bankDBService;
  @Autowired
  private HttpServletContext httpServletContext;
  @Autowired
  private BankDBService bankDBService;
  @Autowired
  private JwtDecoder jwtDecoder;
  // @Autowired
  // private ApplicationProperties applicationProperties;
  private static final Map<String, List<String>> INSECURE_PATHS = new HashMap<>();
  private static final List<String> AUTHORIZED_URIS = Arrays.asList("/v1/customer/user/authorized",
      "/v1/bank/user/authorized",
      "/v1/merchant/user/authorized", "/v1/brontoo/user/authorized","/v1/customer/bill/oauth2/token");
  static {
    INSECURE_PATHS.put(RequestType.GET.toString(), Arrays.asList("/v1/system/healthcheck",
        "/v1/bank/list", "/v1/misc/countries", "/v1/misc/states", "/v1/misc/dipcoin/categories",
        "/v1/customer/app/version", "/v1/customer/toll/ihmclBank", "/v1/merchant/resend/email", "/v1/bank/resend/email",
        "/v1/system/heapMemoryCheck", "/RespVehicleDetails","/v1/customer/bill/oauth2/token","/v1/customer/bill/oauth2/addMoney/user",
        "/v1/customer/bill/healthcheck","/v1/jwt/customer/bill/oauth2/token","/v1/jwt/customer/bill/oauth2/addMoney/user"));
    INSECURE_PATHS.put(RequestType.POST.toString(),
        Arrays.asList("/v1/customer/user/register", "/v1/customer/user/login", "/v1/customer/user/verify",
            "/v1/customer/user/reset", "/v1/merchant/onboard", "/v1/merchant/user/register",
            "/v1/merchant/user/login", "/v1/merchant/user/verify", "/v1/merchant/user/reset",
            "/v1/bank/onboard",
            "/v1/bank/user/register", "/v1/bank/user/login", "/v1/bank/user/verify", "/v1/bank/user/reset",
            "/v1/brontoo/user/register", "/v1/brontoo/user/login", "/v1/brontoo/user/verify",
            "/v1/brontoo/user/reset", "/v1/system/deployment", "/v1/callback/dipcoin/approval",
            "/v1/misc/email", "/v1/misc/script/emails",
            "/v1/customer/user/device/validate", "/reqPayService", "/v1/customer/user/login/pin",
            "/heartBeat", "/v1/brontoo/admin/initiateFastagTxn",
            "/getExceptionResponse", "/queryExceptionResponse", "/v1/bank/user/verifyEmail",
            "/v1/bank/user/bankSubUser",
            "/v1/customer/user/reset/pin", "/v1/customer/user/verify/account/details", "/v1/customer/user/update/pin",
            "/v1/customer/user/delete", "/v1/customer/user/verify/email", "/v1/merchant/user/verify/email",
            "/v1/bank/user/verify/email", "/v1/brontoo/user/verify/email", "/v1/customer/user/verify/account",
            "/successManageTagEntry", "/NETCNotification", "/declineManageTagEntry", "/RespVehicleDetails",
            "/tagEntryServiceResponse","/v1/customer/bill/oauth2/token","/v1/customer/bill/oauth2/addMoney/user", "/v1/customer/user/jwt/login", "/v1/customer/user/jwt/refresh", "/v1/customer/user/jwt/logout"
            ,"/v1/jwt/customer/bill/oauth2/addMoney/user","/v1/jwt/customer/bill/oauth2/token"));
  }
  private static final List<String> ADMIN_OLNY_PATHS = Arrays.asList("/v1/admin/.*", "/v1/system/.*");
  private boolean isSourceWeb;
  @Autowired
  private UserServiceClient userServiceClient;

  @Override
  public Optional<ResponseEntity<String>> intercept(HttpServletRequest request) throws Exception {
    String path = java.util.Optional.ofNullable(request.getPathInfo()).orElse(request.getRequestURI());
    String method = request.getMethod();
    // validate paths to skip user authorization
    /*
     * for (int i = 0; i < INSECURE_PATHS.size(); i++) { if
     * (Pattern.matches(INSECURE_PATHS.get(i), path)) { return; } }
     */
    // check if request doesnt require login
    if (!APIConstants.HEALTHCHECK_API.equals(path) && !APIConstants.SWAGGER_API.equals(path)
        && !APIConstants.HEAPCHECK_API.equals(path)) {
      log.debug(LogFormatter.instance(httpServletContext.getTraceId()).message("Authorizing User")
          .data("Method", method).data("Path", path)
          .data("Query", httpServletContext.getServletRequest().getQueryString()).format());
    }

    if (INSECURE_PATHS.containsKey(method) && INSECURE_PATHS.get(method).contains(path)) {
      if (!APIConstants.HEALTHCHECK_API.equals(path) || !APIConstants.HEAPCHECK_API.equals(path)) {
        log.debug(LogFormatter.instance(httpServletContext.getTraceId()).data("Insecure Path", path)
            .format());
      }
      return Optional.empty();
    }

    // validate user login and session.
    log.debug(LogFormatter.instance(httpServletContext.getTraceId())
        .message("Validate and Initialize User").format());

    String authHeader = request.getHeader("Authorization");

    // Check which authentication method is being used
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      // ============= JWT BEARER TOKEN AUTHENTICATION =============
      log.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Using JWT Bearer token authentication").format());

      try {
        String token = authHeader.substring(7);
        Jwt jwt = jwtDecoder.decode(token);

        // Log token info
        log.info(LogFormatter.instance(httpServletContext.getTraceId())
            .message("User Info from JWT Token")
            .data("Claims", jwt.getClaims()).format());

        String sub = jwt.getSubject();
        if (sub == null) {
          return Optional.of(ResponseEntity.status(HttpStatus.SC_FORBIDDEN)
              .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED).toString()));
        }

        User user = new User();
        try {
          user.setId(Integer.parseInt(sub));
        } catch (NumberFormatException nfe) {
          return Optional.of(ResponseEntity.status(HttpStatus.SC_FORBIDDEN)
              .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED).toString()));
        }

        // Populate additional user info from token
        String phone = jwt.getClaimAsString("phone");
        if (phone != null) {
          user.setPhone(phone);
        }

        String role = jwt.getClaimAsString("role");
        if (role != null) {
          user.setRole(role);
        }

        String email = jwt.getClaimAsString("email");
        if (email != null) {
          user.setEmail(email);
        }

        Object bankMerchantIdClaim = jwt.getClaim("bankMerchantId");
        if (bankMerchantIdClaim instanceof Number) {
            user.setBankMerchantId(((Number) bankMerchantIdClaim).intValue());
        }

        // Fetch full user from database to ensure status and roles are current
        User fullUser = userDBService.getUser(email, phone);

        if (fullUser == null) {
            log.warn(LogFormatter.instance(httpServletContext.getTraceId())
                .message("User from JWT not found in database")
                .data("UserId", user.getId())
                .format());

            return Optional.of(ResponseEntity.status(HttpStatus.SC_FORBIDDEN)
                .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED).toString()));
        }

        // preserve bankMerchantId from JWT
        if (bankMerchantIdClaim instanceof Number) {
            fullUser.setBankMerchantId(((Number) bankMerchantIdClaim).intValue());
        }

        // preserve role from JWT
        Object roleClaim = jwt.getClaim("role");
        if (roleClaim instanceof String) {
            fullUser.setRole((String) roleClaim);
        }

        // set user in context
        httpServletContext.setUser(fullUser);

        // set bank in context
        if (fullUser.getBankMerchantId() > 0) {

            Bank bank = bankDBService.getBank(fullUser.getBankMerchantId());

            if (bank != null) {
                httpServletContext.setBank(bank);

                log.info(LogFormatter.instance(httpServletContext.getTraceId())
                    .message("Bank set in context")
                    .data("BankId", bank.getId())
                    .data("BankIIN", bank.getIin())
                    .format());
            }
        }
        log.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("JWT validated and user set in context")
            .data("UserId", fullUser.getId())
            .data("MerchantId", merchant != null ? merchant.getId() : null)
            .data("BankId", bank != null ? bank.getId() : null)
            .format());

        log.info(LogFormatter.instance(httpServletContext.getTraceId())
            .message("JWT Token Details Exposed")
            .data("UserId", fullUser.getId())
            .data("Phone", fullUser.getPhone())
            .data("Role", fullUser.getRole())
            .data("Email", fullUser.getEmail())
            .data("BankMerchantId", fullUser.getBankMerchantId())
            .format());

        // Validate scopes for JWT
        java.util.List<String> scopes = new java.util.ArrayList<>();
        Object scopeClaim = jwt.getClaim("scope");
        if (scopeClaim instanceof String) {
          String s = (String) scopeClaim;
          if (s != null && !s.isEmpty()) {
            scopes.addAll(java.util.Arrays.asList(s.split(" ")));
          }
        }
        Object scpClaim = jwt.getClaim("scp");
        if (scpClaim instanceof java.util.Collection<?>) {
          for (Object o : (java.util.Collection<?>) scpClaim) {
            if (o != null)
              scopes.add(o.toString());
          }
        }

        String requiredScope;
        boolean isAdminPath = false;
        for (int i = 0; i < ADMIN_OLNY_PATHS.size(); i++) {
          if (java.util.regex.Pattern.matches(ADMIN_OLNY_PATHS.get(i), path)) {
            isAdminPath = true;
            break;
          }
        }

        if (isAdminPath) {
          requiredScope = "bbps:admin";
          boolean hasAdmin = scopes.contains("admin") || scopes.contains("bbps:admin") || scopes.contains("bbps.admin");
          if (!hasAdmin) {
            return Optional.of(ResponseEntity.status(HttpStatus.SC_FORBIDDEN)
                .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED).toString()));
          }
        } else {
          requiredScope = (RequestType.GET.toString().equals(method)) ? "bbps:read" : "bbps:write";
          boolean hasScope = scopes.contains(requiredScope) || scopes.contains(requiredScope.replace(':', '.'));
          if (!hasScope) {
            return Optional.of(ResponseEntity.status(HttpStatus.SC_FORBIDDEN)
                .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED).toString()));
          }
        }

        // JWT authentication successful
        return Optional.empty();

      } catch (Exception e) {
        log.warn(LogFormatter.instance(httpServletContext.getTraceId())
            .message("JWT validation failed").format(), e);
        return Optional.of(ResponseEntity.status(HttpStatus.SC_FORBIDDEN)
            .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED).toString()));
      }

    } else if (authHeader != null && authHeader.startsWith("Token ")) {
      // ============= COOKIE + TOKEN AUTHENTICATION =============
      log.debug(LogFormatter.instance(httpServletContext.getTraceId())
          .message("Using Cookie+Token authentication").format());

      try {
        // Get the DC cookie from the request
        String dcCookie = null;
        javax.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
          for (javax.servlet.http.Cookie cookie : cookies) {
            if (APIConstants.DC_LOGIN_COOKIE.equals(cookie.getName())) {
              dcCookie = cookie.getValue();
              break;
            }
          }
        }

        if (dcCookie == null || dcCookie.isEmpty()) {
          log.warn(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Cookie+Token auth failed: DC cookie not found").format());
          return Optional.of(ResponseEntity.status(HttpStatus.SC_FORBIDDEN)
              .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED).toString()));
        }

        // Call user service to validate Token+Cookie and get user info
        log.debug(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Calling User Service to validate session")
            .data("AuthHeader", authHeader)
            .format());

        com.dipcoin.api.model.UserInfoResponse userInfo = userServiceClient.getUser(authHeader, dcCookie);

        if (userInfo == null || userInfo.getUserId() <= 0) {
          log.warn(LogFormatter.instance(httpServletContext.getTraceId())
              .message("Cookie+Token auth failed: User service returned null or invalid user").format());
          return Optional.of(ResponseEntity.status(HttpStatus.SC_FORBIDDEN)
              .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED).toString()));
        }

        // Map UserInfoResponse to User and set in context
        User user = new User();
        user.setId(userInfo.getUserId());
        user.setFirstName(userInfo.getFname());
        user.setLastName(userInfo.getLname());
        user.setEmail(userInfo.getEmail());
        user.setPhone(userInfo.getPhonenum());
        user.setRole(userInfo.getRole());

        httpServletContext.setUser(user);

        log.info(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Cookie+Token authentication successful")
            .data("UserId", user.getId())
            .data("Email", user.getEmail())
            .data("Phone", user.getPhone())
            .format());

        // Cookie+Token authentication successful
        return Optional.empty();

      } catch (Exception e) {
        log.warn(LogFormatter.instance(httpServletContext.getTraceId())
            .message("Cookie+Token authentication failed").format(), e);
        return Optional.of(ResponseEntity.status(HttpStatus.SC_FORBIDDEN)
            .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED).toString()));
      }

    } else {
      // No valid authentication header found
      log.warn(LogFormatter.instance(httpServletContext.getTraceId())
          .message("No valid Authorization header found")
          .data("AuthHeader", authHeader)
          .format());
      return Optional.of(ResponseEntity.status(HttpStatus.SC_FORBIDDEN)
          .body(APIResponse.error(HeaderCode.USER_UNAUTHORIZED).toString()));
    }
  }

  public boolean checkSource() {
    return this.isSourceWeb;
  }
}
