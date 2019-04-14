package org.myrobotlab.codec;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.myrobotlab.framework.MRLListener;
import org.myrobotlab.framework.Message;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * handles all encoding and decoding of MRL messages or api(s) assumed context -
 * services can add an assumed context as a prefix
 * /api/returnEncoding/inputEncoding/service/method/param1/param2/ ...
 * 
 * xmpp for example assumes (/api/string/gson)/service/method/param1/param2/ ...
 * 
 * scheme = alpha *( alpha | digit | "+" | "-" | "." ) Components of all URIs: [
 * &lt;scheme&gt;:]&lt;scheme-specific-part&gt;[#&lt;fragment&gt;]
 * http://stackoverflow.com/questions/3641722/valid-characters-for-uri-schemes
 * 
 * branch API test 5
 */
public class CodecUtils {

  public final static Logger log = LoggerFactory.getLogger(CodecUtils.class);

  // uri schemes
  public final static String SCHEME_MRL = "mrl";

  public final static String SCHEME_BASE64 = "base64";

  // TODO change to mime-type
  public final static String TYPE_MESSAGES = "messages";
  public final static String TYPE_JSON = "json";
  public final static String TYPE_URI = "uri";

  // mime-types
  // public final static String MIME_TYPE_JSON = "application/json";
  // public final static String MIME_TYPE_MRL_JSON = "application/mrl-json";
  public final static String MIME_TYPE_JSON = "application/json";

  // disableHtmlEscaping to prevent encoding or "=" -
  // private transient static Gson gson = new
  // GsonBuilder().setDateFormat("yyyy-MM-dd
  // HH:mm:ss.SSS").setPrettyPrinting().disableHtmlEscaping().create();
  private transient static Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").setPrettyPrinting().disableHtmlEscaping().create();
  // FIXME - switch to Jackson

  private static boolean initialized = false;

  public final static String makeFullTypeName(String type) {
    if (type == null) {
      return null;
    }
    if (!type.contains(".")) {
      return String.format("org.myrobotlab.service.%s", type);
    }
    return type;
  }

  public static final Set<Class<?>> WRAPPER_TYPES = new HashSet<Class<?>>(
      Arrays.asList(Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Void.class));

  public static final Set<String> WRAPPER_TYPES_CANONICAL = new HashSet<String>(
      Arrays.asList(Boolean.class.getCanonicalName(), Character.class.getCanonicalName(), Byte.class.getCanonicalName(), Short.class.getCanonicalName(),
          Integer.class.getCanonicalName(), Long.class.getCanonicalName(), Float.class.getCanonicalName(), Double.class.getCanonicalName(), Void.class.getCanonicalName()));

  final static HashMap<String, Method> methodCache = new HashMap<String, Method>();

  /**
   * a method signature map based on name and number of methods - the String[]
   * will be the keys into the methodCache A method key is generated by input
   * from some encoded protocol - the method key is object name + method name +
   * parameter number - this returns a full method signature key which is used
   * to look up the method in the methodCache
   */
  final static HashMap<String, ArrayList<Method>> methodOrdinal = new HashMap<String, ArrayList<Method>>();

  final static HashSet<String> objectsCached = new HashSet<String>();

  final static HashMap<String, String> keyToMimeType = new HashMap<String, String>();

  public static final Message base64ToMsg(String base64) {
    String data = base64;
    if (base64.startsWith(String.format("%s://", SCHEME_BASE64))) {
      data = base64.substring(SCHEME_BASE64.length() + 3);
    }
    final ByteArrayInputStream dataStream = new ByteArrayInputStream(Base64.decodeBase64(data));
    try {
      final ObjectInputStream objectStream = new ObjectInputStream(dataStream);
      Message msg = (Message) objectStream.readObject();
      return msg;
    } catch (Exception e) {
      Logging.logError(e);
      return null;
    }
  }

  public static final String capitalize(final String line) {
    return Character.toUpperCase(line.charAt(0)) + line.substring(1);
  }

  public final static <T extends Object> T fromJson(String json, Class<T> clazz) {
    return gson.fromJson(json, clazz);
  }

  public final static <T extends Object> T fromJson(String json, Class<?> generic, Class<?>... parameterized) {
    return gson.fromJson(json, getType(generic, parameterized));
  }

  public final static <T extends Object> T fromJson(String json, Type type) {
    return gson.fromJson(json, type);
  }

  public static Type getType(final Class<?> rawClass, final Class<?>... parameterClasses) {
    return new ParameterizedType() {
      @Override
      public Type[] getActualTypeArguments() {
        return parameterClasses;
      }

      @Override
      public Type getRawType() {
        return rawClass;
      }

      @Override
      public Type getOwnerType() {
        return null;
      }

    };
  }

  static public final byte[] getBytes(Object o) throws IOException {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream(5000);
    ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(byteStream));
    os.flush();
    os.writeObject(o);
    os.flush();
    return byteStream.toByteArray();
  }

  static public final String getCallbackTopicName(String topicMethod) {
    // replacements
    if (topicMethod.startsWith("publish")) {
      return String.format("on%s", capitalize(topicMethod.substring("publish".length())));
    } else if (topicMethod.startsWith("get")) {
      return String.format("on%s", capitalize(topicMethod.substring("get".length())));
    }

    // no replacement - just pefix and capitalize
    // FIXME - subscribe to onMethod --- gets ---> onOnMethod :P
    return String.format("on%s", capitalize(topicMethod));
  }

  // concentrator data coming from decoder
  static public Method getMethod(String serviceType, String methodName, Object[] params) {
    return getMethod("org.myrobotlab.service", serviceType, methodName, params);
  }

  // real encoded data ??? getMethodFromXML getMethodFromJson - all resolve to
  // this getMethod with class form
  // encoded data.. YA !
  static public Method getMethod(String pkgName, String objectName, String methodName, Object[] params) {
    String fullObjectName = String.format("%s.%s", pkgName, objectName);
    log.debug("Full Object Name : {}", fullObjectName);
    return null;
  }

  static public ArrayList<Method> getMethodCandidates(String serviceType, String methodName, int paramCount) {
    if (!objectsCached.contains(serviceType)) {
      loadObjectCache(serviceType);
    }

    String ordinalKey = makeMethodOrdinalKey(serviceType, methodName, paramCount);
    if (!methodOrdinal.containsKey(ordinalKey)) {
      log.error("cant find matching method candidate for {}.{} {} params", serviceType, methodName, paramCount);
      return null;
    }
    return methodOrdinal.get(ordinalKey);
  }

  // TODO
  // public static Object encode(Object, encoding) - dispatches appropriately

  static final public String getMsgKey(Message msg) {
    return String.format("msg %s.%s --> %s.%s(%s) - %d", msg.sender, msg.sendingMethod, msg.name, msg.method, CodecUtils.getParameterSignature(msg.data), msg.msgId);
  }

  static final public String getMsgTypeKey(Message msg) {
    return String.format("msg %s.%s --> %s.%s(%s)", msg.sender, msg.sendingMethod, msg.name, msg.method, CodecUtils.getParameterSignature(msg.data));
  }

  static final public String getParameterSignature(final Object[] data) {
    if (data == null) {
      return "";
    }

    StringBuffer ret = new StringBuffer();
    for (int i = 0; i < data.length; ++i) {
      if (data[i] != null) {
        Class<?> c = data[i].getClass(); // not all data types are safe
        // toString() e.g.
        // SerializableImage
        if (c == String.class || c == Integer.class || c == Boolean.class || c == Float.class || c == MRLListener.class) {
          ret.append(data[i].toString());
        } else {
          String type = data[i].getClass().getCanonicalName();
          String shortTypeName = type.substring(type.lastIndexOf(".") + 1);
          ret.append(shortTypeName);
        }

        if (data.length != i + 1) {
          ret.append(",");
        }
      } else {
        ret.append("null");
      }

    }
    return ret.toString();

  }

  static public String getServiceType(String inType) {
    if (inType == null) {
      return null;
    }
    if (inType.contains(".")) {
      return inType;
    }
    return String.format("org.myrobotlab.service.%s", inType);
  }

  public static Message gsonToMsg(String gsonData) {
    return gson.fromJson(gsonData, Message.class);
  }

  /**
   * most lossy protocols need conversion of parameters into correctly typed
   * elements this method is used to query a candidate method to see if a simple
   * conversion is possible
   * 
   * @param clazz
   *          the class
   * @return true/false
   */
  public static boolean isSimpleType(Class<?> clazz) {
    return WRAPPER_TYPES.contains(clazz) || clazz == String.class;
  }

  public static boolean isWrapper(Class<?> clazz) {
    return WRAPPER_TYPES.contains(clazz);
  }

  public static boolean isWrapper(String className) {
    return WRAPPER_TYPES_CANONICAL.contains(className);
  }

  // FIXME - axis's Method cache - loads only requested methods
  // this would probably be more gracefull than batch loading as I am doing..
  // http://svn.apache.org/repos/asf/webservices/axis/tags/Version1_2RC2/java/src/org/apache/axis/utils/cache/MethodCache.java
  static public void loadObjectCache(String serviceType) {
    try {
      objectsCached.add(serviceType);
      Class<?> clazz = Class.forName(serviceType);
      Method[] methods = clazz.getMethods();
      for (int i = 0; i < methods.length; ++i) {
        Method m = methods[i];
        Class<?>[] types = m.getParameterTypes();

        String ordinalKey = makeMethodOrdinalKey(serviceType, m.getName(), types.length);
        String methodKey = makeMethodKey(serviceType, m.getName(), types);

        if (!methodOrdinal.containsKey(ordinalKey)) {
          ArrayList<Method> keys = new ArrayList<Method>();
          keys.add(m);
          methodOrdinal.put(ordinalKey, keys);
        } else {
          methodOrdinal.get(ordinalKey).add(m);
        }

        if (log.isDebugEnabled()) {
          log.debug("loading {} into method cache", methodKey);
        }
        methodCache.put(methodKey, m);
      }
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  // FIXME !!! - encoding for Message ----> makeMethodKey(Message msg)

  static public String makeMethodKey(String fullObjectName, String methodName, Class<?>[] paramTypes) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < paramTypes.length; ++i) {
      sb.append("/");
      sb.append(paramTypes[i].getCanonicalName());
    }
    return String.format("%s/%s%s", fullObjectName, methodName, sb.toString());
  }

  static public String makeMethodOrdinalKey(String fullObjectName, String methodName, int paramCount) {
    return String.format("%s/%s/%d", fullObjectName, methodName, paramCount);
  }

  // LOSSY Encoding (e.g. xml &amp; gson - which do not encode type information)
  // can possibly
  // give us the parameter count - from the parameter count we can grab method
  // candidates
  // @return is a arraylist of keys !!!

  public static final String msgToBase64(Message msg) {
    final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
    try {
      final ObjectOutputStream objectStream = new ObjectOutputStream(dataStream);
      objectStream.writeObject(msg);
      objectStream.close();
      dataStream.close();
      String base64 = String.format("%s://%s", SCHEME_BASE64, new String(Base64.encodeBase64(dataStream.toByteArray())));
      return base64;
    } catch (Exception e) {
      log.error("couldnt seralize {}", msg);
      Logging.logError(e);
      return null;
    }
  }

  public static String msgToGson(Message msg) {
    return gson.toJson(msg, Message.class);
  }

  public static boolean setJSONPrettyPrinting(boolean b) {
    if (b) {
      gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").setPrettyPrinting().disableHtmlEscaping().create();
    } else {
      gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").disableHtmlEscaping().create();
    }
    return b;
  }

  // --- xml codec begin ------------------
  // inbound parameters are probably strings or xml bits encoded in some way -
  // need to match
  // ordinal first

  static public String toCamelCase(String s) {
    String[] parts = s.split("_");
    String camelCaseString = "";
    for (String part : parts) {
      camelCaseString = camelCaseString + toCCase(part);
    }
    return String.format("%s%s", camelCaseString.substring(0, 1).toLowerCase(), camelCaseString.substring(1));
  }

  static public String toCCase(String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
  }

  public final static String toJson(Object o) {
    return gson.toJson(o);
  }

  public final static String toJson(Object o, Class<?> clazz) {
    return gson.toJson(o, clazz);
  }

  public static void toJsonFile(Object o, String filename) throws IOException {
    FileOutputStream fos = new FileOutputStream(new File(filename));
    fos.write(gson.toJson(o).getBytes());
    fos.close();
  }

  // === method signatures begin ===

  static public String toUnderScore(String camelCase) {
    return toUnderScore(camelCase, false);
  }

  static public String toUnderScore(String camelCase, Boolean toLowerCase) {

    byte[] a = camelCase.getBytes();
    boolean lastLetterLower = false;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < a.length; ++i) {
      boolean currentCaseUpper = Character.isUpperCase(a[i]);

      Character newChar = null;
      if (toLowerCase != null) {
        if (toLowerCase) {
          newChar = (char) Character.toLowerCase(a[i]);
        } else {
          newChar = (char) Character.toUpperCase(a[i]);
        }
      } else {
        newChar = (char) a[i];
      }

      sb.append(String.format("%s%c", (lastLetterLower && currentCaseUpper) ? "_" : "", newChar));
      lastLetterLower = !currentCaseUpper;
    }

    return sb.toString();

  }

  public static boolean tryParseInt(String string) {
    try {
      Integer.parseInt(string);
      return true;
    } catch (Exception e) {

    }
    return false;
  }

  public static String type(String type) {
    int pos0 = type.indexOf(".");
    if (pos0 > 0) {
      return type;
    }
    return String.format("org.myrobotlab.service.%s", type);
  }

  static final String JSON = "application/javascript";

  // start fresh :P
  // FIXME should probably use a object factory and interface vs static methods
  static public void write(OutputStream out, Object toEncode) throws IOException {
    write(JSON, out, toEncode);
  }

  static public void write(String mimeType, OutputStream out, Object toEncode) throws IOException {
    if (JSON.equals(mimeType)) {
      out.write(gson.toJson(toEncode).getBytes());
      // out.flush();
    } else {
      log.error("write mimeType {} not supported", mimeType);
    }
  }

  public static String getKeyToMimeType(String apiTypeKey) {
    if (!initialized) {
      init();
    }

    String ret = MIME_TYPE_JSON;
    if (keyToMimeType.containsKey(apiTypeKey)) {
      ret = keyToMimeType.get(apiTypeKey);
    }

    return ret;
  }

  // API KEY to MIME TYPES (request or response?)
  private static synchronized void init() {
    keyToMimeType.put("messages", MIME_TYPE_JSON);
    keyToMimeType.put("services", MIME_TYPE_JSON);
    initialized = true;
  }

  public static String getSimpleName(String serviceType) {
    int pos = serviceType.lastIndexOf(".");
    if (pos > -1) {
      return serviceType.substring(pos + 1);
    }
    return serviceType;
  }

  // === method signatures end ===
}
