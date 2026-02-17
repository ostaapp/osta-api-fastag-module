package com.dipcoin.api.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TollTagWihVinDetail  {

    private String firstName;

    private String lastName;

    private String mobileNo;

    private String vin;

    private String serialNumber;

    private int isDocumentUploaded; // Yes, No

    private int isDocumentVerified; // Yes, No

    private int isVrnUpdatedWithNETC;   // Yes, No

    private String vrn;

    private String rcImage;

    private String vehicleImage;

    private long noOfDaysFromDateOfTagApproval; 

}