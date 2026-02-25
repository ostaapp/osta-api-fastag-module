package com.dipcoin.api.fraudMgmt;

import com.dipcoin.db.services.model.UserState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Accessors(chain = true)
@Setter
@Getter
public class ProcessEvent {

    private EventType type;
    private boolean process = true;

    private UserState customer;

    private UserState merchant;

    private String dcoin;

    private String requestTime;

    private String originIp;

    private String errorMsg;



    public enum EventType {
        OstaUsage, OstaCreation,LoginCheck
    }

     @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("ProcessEvent{");
            sb.append("type='").append(type).append('\'');
            sb.append(", process=").append(process);
            sb.append('}');
            return sb.toString();
        }




}
