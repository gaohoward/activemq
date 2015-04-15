package org.apache.activemq.broker.amq6wrapper;

import java.io.Serializable;
import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

/**
 * @author <a href="mailto:ovidiu@feodorov.com">Ovidiu Feodorov</a>
 * @version <tt>$Revision: 2868 $</tt>
 *
 */
public class InVMNameParser implements NameParser, Serializable
{
   // Constants -----------------------------------------------------

   private static final long serialVersionUID = 2925203703371001031L;

   // Static --------------------------------------------------------

   static Properties syntax;

   static
   {
      InVMNameParser.syntax = new Properties();
      InVMNameParser.syntax.put("jndi.syntax.direction", "left_to_right");
      InVMNameParser.syntax.put("jndi.syntax.ignorecase", "false");
      InVMNameParser.syntax.put("jndi.syntax.separator", "/");
   }

   // Attributes ----------------------------------------------------

   // Constructors --------------------------------------------------

   // Public --------------------------------------------------------

   public static Properties getSyntax()
   {
      return InVMNameParser.syntax;
   }

   public Name parse(final String name) throws NamingException
   {
      return new CompoundName(name, InVMNameParser.syntax);
   }

   // Package protected ---------------------------------------------

   // Protected -----------------------------------------------------

   // Private -------------------------------------------------------

   // Inner classes -------------------------------------------------

}
