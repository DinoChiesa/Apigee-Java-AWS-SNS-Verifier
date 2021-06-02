// AbstractCallout.java

package com.google.apigee.callouts;

import com.apigee.flow.message.MessageContext;
import com.google.apigee.util.CalloutUtil;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractCallout {
  protected final Map<String, String> properties;
  protected static String varprefix = "abstract_";
  private static final String variableReferencePatternString = "(.*?)\\{([^\\{\\} ]+?)\\}(.*?)";
  private static final Pattern variableReferencePattern =
      Pattern.compile(variableReferencePatternString);
  private static final String commonError = "^(.+?)[:;] (.+)$";
  private static final Pattern commonErrorPattern = Pattern.compile(commonError);

  public AbstractCallout(Map properties) {
    this.properties = CalloutUtil.genericizeMap(properties);
  }

  protected String resolveVariableReferences(String spec, MessageContext msgCtxt) {
    Matcher matcher = variableReferencePattern.matcher(spec);
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(sb, "");
      sb.append(matcher.group(1));
      String ref = matcher.group(2);
      String[] parts = ref.split(":", 2);
      Object v = msgCtxt.getVariable(parts[0]);
      if (v != null) {
        sb.append((String) v);
      } else if (parts.length > 1) {
        sb.append(parts[1]);
      }
      sb.append(matcher.group(3));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  // private String _getStringProp(MessageContext msgCtxt, String name, String defaultValue) throws
  // Exception {
  //   String value = this.properties.get(name);
  //   if (value != null) value = value.trim();
  //   if (value == null || value.equals("")) {
  //     return defaultValue;
  //   }
  //   value = resolveVariableReferences(value, msgCtxt);
  //   if (value == null || value.equals("")) {
  //     throw new IllegalStateException(name + " resolves to null or empty.");
  //   }
  //   return value;
  // }
  //
  // private String _getRequiredString(MessageContext msgCtxt, String name) throws Exception {
  //   String value = _getStringProp(msgCtxt, name, null);
  //   if (value == null)
  //     throw new IllegalStateException(String.format("%s resolves to null or empty.",name));
  //   return value;
  // }
  //
  // private String _getOptionalString(MessageContext msgCtxt, String name) throws Exception {
  //   return _getStringProp(msgCtxt, name, null);
  // }

  protected boolean _getBooleanProperty(
      MessageContext msgCtxt, String propName, boolean defaultValue) throws Exception {
    String flag = this.properties.get(propName);
    if (flag != null) flag = flag.trim();
    if (flag == null || flag.equals("")) {
      return defaultValue;
    }
    flag = resolveVariableReferences(flag, msgCtxt);
    if (flag == null || flag.equals("")) {
      return defaultValue;
    }
    return flag.equalsIgnoreCase("TRUE");
  }

  protected static String getStackTraceAsString(Throwable t) {
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    t.printStackTrace(pw);
    return sw.toString();
  }

  protected static String varName(String s) {
    return varprefix + s;
  }

  protected void setExceptionVariables(Exception exc1, MessageContext msgCtxt) {
    String error = exc1.toString().replaceAll("\n", " ");
    msgCtxt.setVariable(varName("exception"), error);
    Matcher matcher = commonErrorPattern.matcher(error);
    if (matcher.matches()) {
      msgCtxt.setVariable(varName("error"), matcher.group(2));
    } else {
      msgCtxt.setVariable(varName("error"), error);
    }
  }
}
