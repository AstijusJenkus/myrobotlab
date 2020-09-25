package org.myrobotlab.framework;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * <pre>
 * Command options for picocli library. This encapsulates all the available
 * command line flags and their details. arity attribute is for specifying in
 * an array or list the number of expected attributes after the flag. Short
 * versions of flags e.g. -i must be unique and have only a single character.
 * 
 * FIXME - make it callable so it does a callback and does some post proccessing .. i think that's why its callable ?
 * FIXME - have it capable of toString or buildCmdLine that in turn can be used as input to generate the CmdOptions again, ie.
 *         test serialization
 *         
 * FIXME - there are in parameters - ones supplied by the user of ProcessUtils, and out params which will become 
 * behavior changes in the Runtime
 * 
 * </pre>
 */
@Command(name = "java -jar myrobotlab.jar ")
public class CmdOptions {

  public CmdOptions() {
  }

  // copy constructor for people who don't like continued maintenance ;) -
  // potentially dangerous for arrays and containers
  public CmdOptions(CmdOptions other) throws IllegalArgumentException, IllegalAccessException {
    Field[] fields = this.getClass().getDeclaredFields();
    for (Field field : fields) {
      field.set(this, field.get(other));
    }
  }

  // launcher ??
  @Option(names = { "-a", "--auto-update" }, description = "auto updating - this feature allows mrl instances to be automatically updated when a new version is available")
  public boolean autoUpdate = false;

  public final String DEFAULT_CONNECT = "http://localhost:8888";

  // TODO - daemon / fork
  @Option(names = { "-d", "--daemon" }, description = "daemon - fork process from current process - no inherited io no cli")
  public boolean daemon = false;

  // launcher
  @Option(names = { "-c", "--connect" }, arity = "0..*", /*
                                                          * defaultValue =
                                                          * DEFAULT_CONNECT,
                                                          */ fallbackValue = DEFAULT_CONNECT, description = "connects this mrl instance to another mrl instance - default is "
      + DEFAULT_CONNECT)
  public String connect = null;

  // launcher
  @Option(names = { "--config" }, description = "Configuration file. If specified all configuration from the file will be used as a \"base\" of configuration. "
      + "All configuration of last run is saved to {data-dir}/lastOptions.json. This file can be used as a starter config for subsequent --cfg config.json. "
      + "If this value is set, all other configuration flags are ignored.")
  public String cfg = null;

  // FIXME - highlight or italics for examples !!
  // launcher
  @Option(names = { "-m", "--memory" }, description = "adjust memory can e.g. -m 2g \n -m 128m")
  public String memory = null;

  // launcher
  /*
   * @Option(names = { "--std-out" }, description =
   * "when spawning save the results of the launch in a file \"std.out\"")
   * public boolean stdout = false;
   */

  // if --from-launcher knows to createAndStart service on -s
  @Option(names = { "--from-launcher" }, description = "prevents starting in interactive mode - reading from stdin")
  public boolean fromLauncher = false;

  @Option(names = { "-h", "-?", "--help" }, description = "shows help")
  public boolean help = false;

  @Option(names = { "-I",
      "--invoke" }, arity = "0..*", description = "invokes a method on a service --invoke {serviceName} {method} {param0} {param1} ... : --invoke python execFile myFile.py")
  public String invoke[];

  // FIXME - should work with a startup ...
  @Option(names = { "-k", "--add-key" }, arity = "2..*", description = "adds a key to the key store\n"
      + "@bold,italic java -jar myrobotlab.jar -k amazon.polly.user.key ABCDEFGHIJKLM amazon.polly.user.secret Fidj93e9d9fd88gsakjg9d93")
  public String addKeys[];

  @Option(names = { "-j", "--jvm" }, arity = "0..*", description = "jvm parameters for the instance of mrl")
  public String jvm;

  @Option(names = { "--id" }, description = "process identifier to be mdns or network overlay name for this instance - one is created at random if not assigned")
  public String id;

  @Option(names = { "-l", "--log-level" }, description = "log level - helpful for troubleshooting " + " [debug info warn error]")
  public String logLevel = "info";

  @Option(names = { "-i",
      "--install" }, arity = "0..*", description = "installs all dependencies for all services, --install {serviceType} installs dependencies for a specific service, if no type is specified then all services are installed")
  public String install[];

  @Option(names = { "-V", "--virtual" }, description = "sets global environment as virtual - all services which support virtual hardware will create virtual hardware")
  public boolean virtual = false;

  @Option(names = { "-s", "--service",
      "--services" }, arity = "0..*", description = "services requested on startup, the services must be {name} {Type} paired, e.g. gui SwingGui webgui WebGui servo Servo ...")
  public List<String> services = new ArrayList<>();

}
