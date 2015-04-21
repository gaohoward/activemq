package org.apache.activemq.broker.amq6wrapper;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.activemq.amq6wrapper.Amq6BrokerHelper;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.api.core.SimpleString;
import org.apache.activemq.api.core.TransportConfiguration;
import org.apache.activemq.core.config.Configuration;
import org.apache.activemq.core.config.impl.SecurityConfiguration;
import org.apache.activemq.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.core.security.Role;
import org.apache.activemq.core.settings.impl.AddressSettings;
import org.apache.activemq.spi.core.security.ActiveMQSecurityManagerImpl;

public class ActiveMQBroker6Wrapper extends Amq6BrokerBase {

   protected Map<String, SimpleString> testQueues = new HashMap<String, SimpleString>();

   public ActiveMQBroker6Wrapper(BrokerService brokerService) {
      this.bservice = brokerService;
   }

   @Override
   public void start() throws Exception {
      testDir = temporaryFolder.getRoot().getAbsolutePath();
      clearDataRecreateServerDirs();
      server = createServer(realStore, false);
      HashMap<String, Object> params = new HashMap<String, Object>();
      params.put(TransportConstants.PORT_PROP_NAME, "61616");
      params.put(TransportConstants.PROTOCOLS_PROP_NAME, "OPENWIRE");
      TransportConfiguration transportConfiguration = new TransportConfiguration(NETTY_ACCEPTOR_FACTORY, params);

      Configuration serverConfig = server.getConfiguration();

      Set<TransportConfiguration> acceptors0 = serverConfig.getAcceptorConfigurations();
      Iterator<TransportConfiguration> iter0 = acceptors0.iterator();

      while (iter0.hasNext()) {
         System.out.println("===>: " + iter0.next());
      }

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
         params.put(TransportConstants.PROTOCOLS_PROP_NAME, "OPENWIRE");
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
         ActiveMQSecurityManagerImpl sm = (ActiveMQSecurityManagerImpl)server.getSecurityManager();
         SecurityConfiguration securityConfig = sm.getConfiguration();
         securityConfig.addRole("openwireSender", "sender");
         securityConfig.addUser("openwireSender", "SeNdEr");
         //sender cannot receive
         Role senderRole = new Role("sender", true, false, false, false, true, true, false);

         securityConfig.addRole("openwireReceiver", "receiver");
         securityConfig.addUser("openwireReceiver", "ReCeIvEr");
         //receiver cannot send
         Role receiverRole = new Role("receiver", false, true, false, false, true, true, false);

         securityConfig.addRole("openwireGuest", "guest");
         securityConfig.addUser("openwireGuest", "GuEsT");

         //guest cannot do anything
         Role guestRole = new Role("guest", false, false, false, false, false, false, false);

         securityConfig.addRole("openwireDestinationManager", "manager");
         securityConfig.addUser("openwireDestinationManager", "DeStInAtIoN");

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
      Set<TransportConfiguration> acceptors = serverConfig.getAcceptorConfigurations();
      Iterator<TransportConfiguration> iter = acceptors.iterator();

      while (iter.hasNext()) {
         System.out.println(">: " + iter.next());
      }
      server.start();

/*
	      registerConnectionFactory();
	      mbeanServer = MBeanServerFactory.createMBeanServer();
*/

      Amq6BrokerHelper.setBroker(this.bservice);
      stopped = false;

   }

   @Override
   public void stop() throws Exception {
      server.stop();
      testQueues.clear();
      stopped = true;
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
