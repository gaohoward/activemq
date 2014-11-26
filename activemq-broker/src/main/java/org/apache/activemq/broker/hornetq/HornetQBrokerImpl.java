package org.apache.activemq.broker.hornetq;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.hornetq.HornetQBrokerHelper;
import org.apache.activemq.util.DebugLogger;
import org.apache.activemq.api.core.SimpleString;
import org.apache.activemq.api.core.TransportConfiguration;
import org.apache.activemq.core.config.Configuration;
import org.apache.activemq.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.core.security.Role;
import org.apache.activemq.core.settings.impl.AddressSettings;

public class HornetQBrokerImpl extends HornetQBroker {
	private static final DebugLogger logger = DebugLogger.getLogger("hqbroker.log");

    protected Map<String, SimpleString> testQueues = new HashMap<String, SimpleString>();

	public HornetQBrokerImpl(BrokerService brokerService) {
		this.bservice = brokerService;
	}

	@Override
	public void start() throws Exception {
		logger.log("Starting HornetQ broker...");
		testDir = temporaryFolder.getRoot().getAbsolutePath();
		clearDataRecreateServerDirs();
	      server = createServer(realStore, true);
	      HashMap<String, Object> params = new HashMap<String, Object>();
	      params.put(TransportConstants.PORT_PROP_NAME, "61616");
	      params.put(TransportConstants.PROTOCOLS_PROP_NAME, "OPENWIRE");
	      TransportConfiguration transportConfiguration = new TransportConfiguration(NETTY_ACCEPTOR_FACTORY, params);

	      Configuration serverConfig = server.getConfiguration();

	      Map<String, AddressSettings> addressSettings = serverConfig.getAddressesSettings();
	      String match = "jms.queue.#";
	      AddressSettings dlaSettings = new AddressSettings();
	      SimpleString dla = new SimpleString("jms.queue.ActiveMQ.DLQ");
	      dlaSettings.setDeadLetterAddress(dla);
	      addressSettings.put(match, dlaSettings);

	      serverConfig.getAcceptorConfigurations().add(transportConfiguration);
	      if (this.bservice.enableSsl()) {
	          params = new HashMap<String, Object>();
	          params.put(TransportConstants.SSL_ENABLED_PROP_NAME, true);
	          params.put(TransportConstants.PORT_PROP_NAME, 61611);
	          params.put(TransportConstants.KEYSTORE_PATH_PROP_NAME, bservice.SERVER_SIDE_KEYSTORE);
	          params.put(TransportConstants.KEYSTORE_PASSWORD_PROP_NAME, bservice.KEYSTORE_PASSWORD);
	          params.put(TransportConstants.KEYSTORE_PROVIDER_PROP_NAME, bservice.storeType);
	          if (bservice.SERVER_SIDE_TRUSTSTORE != null) {
		          params.put(TransportConstants.NEED_CLIENT_AUTH_PROP_NAME, true);	        	  
		          params.put(TransportConstants.TRUSTSTORE_PATH_PROP_NAME, bservice.SERVER_SIDE_TRUSTSTORE);
		          params.put(TransportConstants.TRUSTSTORE_PASSWORD_PROP_NAME, bservice.TRUSTSTORE_PASSWORD);
		          params.put(TransportConstants.TRUSTSTORE_PROVIDER_PROP_NAME, bservice.storeType);
	          }
	          TransportConfiguration sslTransportConfig = new TransportConfiguration(NETTY_ACCEPTOR_FACTORY, params);
	          serverConfig.getAcceptorConfigurations().add(sslTransportConfig);
	      }

	      for (Integer port : bservice.extraConnectors) {
	    	  if (port.intValue() != 61616) {
	    		  //extra port
	    	      params = new HashMap<String, Object>();
	    	      params.put(TransportConstants.PORT_PROP_NAME, port.intValue());
	    	      params.put(TransportConstants.PROTOCOLS_PROP_NAME, "OPENWIRE");
	    	      TransportConfiguration extraTransportConfiguration = new TransportConfiguration(NETTY_ACCEPTOR_FACTORY, params);
	    	      serverConfig.getAcceptorConfigurations().add(extraTransportConfiguration);
	    	  }
	      }
	      
	      serverConfig.setSecurityEnabled(enableSecurity);

	      //extraServerConfig(serverConfig);

	      if (enableSecurity)
	      {
	         server.getSecurityManager().addRole("openwireSender", "sender");
	         server.getSecurityManager().addUser("openwireSender", "SeNdEr");
	         //sender cannot receive
	         Role senderRole = new Role("sender", true, false, false, false, true, true, false);

	         server.getSecurityManager().addRole("openwireReceiver", "receiver");
	         server.getSecurityManager().addUser("openwireReceiver", "ReCeIvEr");
	         //receiver cannot send
	         Role receiverRole = new Role("receiver", false, true, false, false, true, true, false);

	         server.getSecurityManager().addRole("openwireGuest", "guest");
	         server.getSecurityManager().addUser("openwireGuest", "GuEsT");

	         //guest cannot do anything
	         Role guestRole = new Role("guest", false, false, false, false, false, false, false);

	         server.getSecurityManager().addRole("openwireDestinationManager", "manager");
	         server.getSecurityManager().addUser("openwireDestinationManager", "DeStInAtIoN");

	         //guest cannot do anything
	         Role destRole = new Role("manager", false, false, false, false, true, true, false);

	         Map<String, Set<Role>> settings = server.getConfiguration().getSecurityRoles();
	         if (settings == null)
	         {
	            settings = new HashMap<String, Set<Role>>();
	            server.getConfiguration().setSecurityRoles(settings);
	         }
	         Set<Role> anySet = settings.get("#");
	         if (anySet == null)
	         {
	            anySet = new HashSet<Role>();
	            settings.put("#", anySet);
	         }
	         anySet.add(senderRole);
	         anySet.add(receiverRole);
	         anySet.add(guestRole);
	         anySet.add(destRole);
	      }
/* no need to start jms server here
	      jmsServer = new JMSServerManagerImpl(server);
	      jmsServer.setContext(new InVMNamingContext());
	      jmsServer.start();
*/
	      server.start();

/*
	      registerConnectionFactory();
	      mbeanServer = MBeanServerFactory.createMBeanServer();
*/
	      logger.log("debug: server started");

	      HornetQBrokerHelper.setBroker(this.bservice);
		stopped = false;

	}

	@Override
	public void stop() throws Exception {
        logger.log("stopping hornetq server....");
        server.stop();
        testQueues.clear();
		stopped = true;
		logger.log("server stopped.");
	}

	public void makeSureQueueExists(String qname) throws Exception {
		synchronized (testQueues) {
            SimpleString coreQ = testQueues.get(qname);
	        if (coreQ == null)
	        {
	           coreQ = new SimpleString("jms.queue." + qname);
	           this.server.createQueue(coreQ, coreQ, null, false, false);
	           testQueues.put(qname, coreQ);
            }
		}
	}
}
