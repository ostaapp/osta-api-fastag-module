package com.dipcoin.partner.utils;

import java.util.Random;

public class RandomGenerator {
  private static final Random generator = new Random();
  
  public static String generateRandom() {
    int positiveRandInt = generator.nextInt(2147483647);
    return String.valueOf(positiveRandInt);
  }
  
  public static String generateRandomSixDigits() {
    int min = 100000, max = 999999;
    int randomNum = generator.nextInt(max - min + 1) + min;
    return String.valueOf(randomNum);
  }
  
  public static String randomAccountNumber() {
    int min = 100000, max = 1232100000;
    int randomAccount = (int)(Math.random() * min + max);
    return String.valueOf(randomAccount);
  }
}
