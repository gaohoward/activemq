package org.apache.activemq.amq6wrapper;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;

import org.apache.activemq.command.ActiveMQDestination;

public class Amq6BrokerHelper {

   private static volatile Object service = null;
   private static Class<?> serviceClass;

   static {
      try {
         serviceClass = Class.forName("org.apache.activemq.broker.BrokerService");
      } catch (ClassNotFoundException e) {
         e.printStackTrace();
      }

   }
   // start a tcp transport hornetq broker, the broker need to
   // be invm with client.
   public static void startHornetQBroker(URI location) throws IOException {
      if (service != null) {
         return;
      }
      try {
         service = serviceClass.newInstance();
         Method startMethod = serviceClass.getMethod("start");
         startMethod.invoke(service, (Object[]) null);
      } catch (InstantiationException e) {
         throw new IOException("Inst exception", e);
      } catch (IllegalAccessException e) {
         throw new IOException("IllegalAccess exception ", e);
      } catch (NoSuchMethodException e) {
         throw new IOException("Nosuchmethod", e);
      } catch (SecurityException e) {
         throw new IOException("Security exception", e);
      } catch (IllegalArgumentException e) {
         throw new IOException("IllegalArgumentException exception", e);
      } catch (InvocationTargetException e) {
         throw new IOException("InvocationTargetException exception", e);
      }
   }

   public static void makeSureDestinationExists(ActiveMQDestination activemqDestination) throws Exception {
      Method startMethod = serviceClass.getMethod("makeSureDestinationExists", ActiveMQDestination.class);
      startMethod.invoke(service, activemqDestination);
   }

   //some tests run broker in setUp(). This need be called
   //to prevent auto broker creation.
   public static void setBroker(Object startedBroker) {
      service = startedBroker;
   }

}

