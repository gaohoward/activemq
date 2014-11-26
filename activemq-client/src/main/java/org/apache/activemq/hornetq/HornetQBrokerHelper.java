package org.apache.activemq.hornetq;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;

import org.apache.activemq.command.ActiveMQDestination;
import org.apache.activemq.util.DebugLogger;

public class HornetQBrokerHelper {
	private static final DebugLogger logger = DebugLogger.getLogger("hqbroker.log");
	private static volatile Object service = null;
	private static Class<?> serviceClass;

	static {
		try {
			serviceClass = Class.forName("org.apache.activemq.broker.BrokerService");
		} catch (ClassNotFoundException e) {
			logger.log("CNF exception, remote client? ", false, e);
		}
		
	}
	// start a tcp transport hornetq broker, the broker need to
	// be invm with client.
	public static void startHornetQBroker(URI location) throws IOException {
		if (service != null) {
			logger.log("do not start another broker while it's already one.");
			return;
		}
		try {
			service = serviceClass.newInstance();
			logger.log("found and create service obj: " + service);
			Method startMethod = serviceClass.getMethod("start");
			startMethod.invoke(service, (Object[]) null);
			logger.log("hornetq broker started successfuly.");
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
			logger.log("error", false, e);
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
