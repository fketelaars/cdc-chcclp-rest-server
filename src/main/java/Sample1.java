import com.ibm.replication.cdc.scripting.EmbeddedScript;
import com.ibm.replication.cdc.scripting.EmbeddedScriptException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Sample1
{
   public static void main(String[] args)
   {
      Map<String, String> env = loadDotEnv();

      String hostname = required(env, "CONNECT_HOSTNAME");
      String port     = required(env, "CONNECT_PORT");
      String username = required(env, "CONNECT_USERNAME");
      String password = required(env, "CONNECT_PASSWORD");

      EmbeddedScript script = new EmbeddedScript();

      try
      {
         script.open();

         script.execute(String.format(
               "connect server hostname %s port %s username %s password %s;",
               hostname, port, username, password));
         script.execute("connect datastore name Db2LUW context source;");
         script.execute("connect datastore name Kafka context target;");
         script.execute("list subscriptions filter datastore;");
         System.out.println(script.getResult());
         script.execute("select subscription name FK1;");
         script.execute("list table mappings;");
         System.out.println(script.getResult());

         // script.execute("add subscription name SUB1;");

         // String mapping = "add table mapping "
         //    + "sourceSchema {0} sourceTable {1} "
         //    + "targetSchema {2} targetTable {3};";

         // script.execute(MessageFormat.format(mapping, new Object[] {
         //    "USER1", "TABLE_1", "USER1", "TABLE_1" }));
         // script.execute(MessageFormat.format(mapping, new Object[] {
         //    "USER1", "TABLE_2", "USER1", "TABLE_2" }));
         // script.execute(MessageFormat.format(mapping, new Object[] {
         //    "USER1", "TABLE_3", "USER1", "TABLE_3" }));

         // script.execute("start mirroring;");
         script.execute("disconnect server;");
      }
      catch (EmbeddedScriptException e)
      {
         System.out.println(e.getResultCodeAndMessage());
      }
      finally
      {
         script.close();
      }
   }

   // ---------------------------------------------------------------------------
   // .env loader
   // ---------------------------------------------------------------------------

   /**
    * Reads a {@code .env} file from the current working directory and returns
    * its key=value pairs. Lines starting with {@code #} and blank lines are
    * ignored. Inline comments (after {@code #}) and surrounding quotes are
    * stripped. Environment variables already set in the process take precedence.
    */
   private static Map<String, String> loadDotEnv()
   {
      Map<String, String> result = new HashMap<>();
      try
      {
         for (String line : Files.readAllLines(Paths.get(".env")))
         {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq < 1) continue;
            String key   = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            // Strip inline comment
            int hash = value.indexOf(" #");
            if (hash >= 0) value = value.substring(0, hash).trim();
            // Strip surrounding quotes
            if (value.length() >= 2
                  && ((value.startsWith("\"") && value.endsWith("\""))
                  ||  (value.startsWith("'")  && value.endsWith("'"))))
            {
               value = value.substring(1, value.length() - 1);
            }
            result.put(key, value);
         }
      }
      catch (IOException e)
      {
         System.err.println("Warning: could not read .env file: " + e.getMessage());
      }
      return result;
   }

   /**
    * Returns the value for {@code key}: process environment first, then
    * {@code .env} file. Exits with an error message if neither is set.
    */
   private static String required(Map<String, String> dotEnv, String key)
   {
      String value = System.getenv(key);
      if (value == null || value.isBlank()) value = dotEnv.get(key);
      if (value == null || value.isBlank())
      {
         System.err.println("Error: required variable " + key + " is not set in .env or environment.");
         System.exit(1);
      }
      return value;
   }
}