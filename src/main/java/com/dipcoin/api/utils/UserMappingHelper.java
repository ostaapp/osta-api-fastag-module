package com.dipcoin.api.utils;

import com.dipcoin.api.model.UserInfoResponse;
import com.dipcoin.db.services.model.User;

/**
 * Helper utility class for mapping UserInfoResponse to User entity
 */
public class UserMappingHelper {

    // Private constructor to prevent instantiation
    private UserMappingHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Helper method to safely set a value if source is not null
     * 
     * @param source Source value to check
     * @param setter Consumer to set the value
     */
    private static <T> void setIfNotNull(T source, java.util.function.Consumer<T> setter) {
        if (source != null) {
            setter.accept(source);
        }
    }

    /**
     * Maps UserInfoResponse to User entity
     * 
     * @param resp UserInfoResponse from user service
     * @return User entity or null if response is null
     */
    public static User mapUserInfoResponseToUser(UserInfoResponse resp) {
        if (resp == null) {
            return null;
        }

        User user = new User();

        user.setId(resp.getUserId());

        if (resp.getExpertise() != null) {
            user.setExpertise(resp.getExpertise());
        }
        if (resp.getStatus() != null) {
            user.setStatus(resp.getStatus());
        }
        if (resp.getTnc() != null) {
            user.setTnC(resp.getTnc());
        }

        setIfNotNull(resp.getFname(), user::setFirstName);
        setIfNotNull(resp.getLname(), user::setLastName);
        setIfNotNull(resp.getEmail(), user::setEmail);
        setIfNotNull(resp.getPhonenum(), user::setPhone);
        setIfNotNull(resp.getPhoneAlias(), user::setPhoneAlias);
        setIfNotNull(resp.getRole(), user::setRole);
        setIfNotNull(resp.getRegistrationDate(), user::setRegistrationDate);
        setIfNotNull(resp.getAgentId(), user::setAgentId);
        setIfNotNull(resp.getClientTransactionId(), user::setClientTransactionId);

        return user;
    }
}