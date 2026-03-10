package com.dipcoin.partner.toll.commons;

import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import lombok.Getter;

@Getter
public enum VehicleCategories {


//@formatter:off
  CAR_JEEP_VAN( 1, "VC4",  "Car / Jeep / Van",  "Violet", "004" ),
  TATA_ACE_MINI_LIGHT_COMMERCIAL( 2, "VC4",  "Tata Ace and Similar mini Light Commercial Vehicle",  "Violet", "004" ),
  LIGHT_COMMERCIAL_VEHICLE_MINI_BUS( 3, "VC5",  "Light Commercial vehicle 2-axle / Mini Bus",  "Violet", "005" ),
  TRUCK_3_AXLE( 4, "VC6",  "Truck 3-axle",  "Violet", "006" ),
  BUS_3_AXLE( 5, "VC6",  "Bus 3-axle",  "Violet", "006" ),  
  BUS_2_AXEL( 6, "VC7",  "Bus 2-axle",  "Violet", "007" ),
  TRUCK_2_AXEL( 7, "VC7",  "Truck 2-axle",  "Violet", "007" ),
  TRACTOR_WITH_TRAILER( 8, "VC12",  "Tractor / Tractor with trailer",  "Violet", "012" ),
  TRUCK_4_AXEL( 9, "VC12",  "Truck 4-axle",  "Violet", "012" ),
  TRUCK_5_AXEL( 10, "VC12",  "Truck 5-axle",  "Violet", "012" ),
  TRUCK_6_AXEL( 11, "VC12",  "Truck 6-axle",  "Violet", "012" ),
  TRUCK_MULTI_AXEL_7_ABOVE( 12, "VC15",  "Truck Multi axle ( 7 and above)",  "Violet", "015" ),
  EARTH_MOVING_MACHINERY( 13, "VC16",  "Earth Moving Machinery",  "Violet", "016" );


                  

  //@formatter:on
  private static final Logger LOG = LogManager.getLogger(VehicleCategories.class);


  VehicleCategories(int id, String category, String vehicleInfo, String colour, String categoryId) {
    this.id = id;
    this.category = category;
    this.vehicleInfo = vehicleInfo;
    this.colour = colour;
    this.categoryId = categoryId;


  }

  private int id;
  private String category;
  private String vehicleInfo;
  private String colour;
  private String categoryId;

  public final static Set<String> lookup = new HashSet<>();

  static {

    for (VehicleCategories vehicleCategory : VehicleCategories.values()) {
      if (lookup.contains(vehicleCategory.getVehicleInfo())) {
        LOG.error(String.format("Duplicate code: %s configured. Last instance will be used",
            vehicleCategory.getVehicleInfo()));
      } else {
        lookup.add(vehicleCategory.getVehicleInfo());
      }
    }
  }


  public static boolean contains(String vehicleInfo) {
    return lookup.contains(vehicleInfo);
  }



}
