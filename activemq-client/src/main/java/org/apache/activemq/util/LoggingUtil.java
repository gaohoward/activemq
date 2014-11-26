package org.apache.activemq.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LoggingUtil {

   private static SimpleDateFormat format = new SimpleDateFormat("MM:dd-HH:mm:ss:SSS");

   public static String getCurrentTime() {
      Date d = new Date();
      return format.format(d);
   }
}


