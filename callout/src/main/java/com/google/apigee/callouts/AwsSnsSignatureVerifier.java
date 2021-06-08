// AwsSnsSignatureVerifier.java
//
// This is the main callout class for the AWS SNS Signature Verifier custom policy for Apigee.
// For full details see the Readme accompanying this source file.
//
// Copyright (c) 2021 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// @author: Dino Chiesa
//

package com.google.apigee.callouts;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.IOIntensive;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import com.google.apigee.json.JavaxJson;
import com.google.apigee.util.CertCache;
import com.google.apigee.util.TimeResolver;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@IOIntensive
public class AwsSnsSignatureVerifier extends AbstractCallout implements Execution {
  private static final long DEFAULT_MAX_LIFETIME_SECONDS = 60L;

  static {
    varprefix = "awssns_";
  }

  public AwsSnsSignatureVerifier(Map properties) {
    super(properties);
  }

  private long getMaxAllowableLifetimeInSeconds(MessageContext msgCtxt) throws Exception {
    String maxLifetime = (String) this.properties.get("max-lifetime");
    if (maxLifetime == null) {
      return DEFAULT_MAX_LIFETIME_SECONDS;
    }
    maxLifetime = maxLifetime.trim();
    if (maxLifetime.equals("")) {
      return DEFAULT_MAX_LIFETIME_SECONDS;
    }
    maxLifetime = resolveVariableReferences(maxLifetime, msgCtxt);
    if (maxLifetime == null || maxLifetime.equals("")) {
      return DEFAULT_MAX_LIFETIME_SECONDS;
    }

    Long maxLifetimeInMilliseconds = TimeResolver.resolveExpression(maxLifetime);
    if (maxLifetimeInMilliseconds < 0L) return -1L;
    return (maxLifetimeInMilliseconds / 1000L);
  }

  private static boolean isMessageSignatureValid(Map<String, String> map) {
    try {
      String certUri = map.get("SigningCertURL");
      verifyCertificateURL(certUri);
      Signature sig = Signature.getInstance("SHA1withRSA");
      sig.initVerify(CertCache.getCert(certUri).getPublicKey());
      sig.update(getMessageBytesToSign(map));
      return sig.verify(Base64.getDecoder().decode((String) map.get("Signature")));
    } catch (Exception e) {
      throw new SecurityException("Verify failed", e);
    }
  }

  private static void verifyCertificateURL(String signingCertUri) {
    URI certUri = URI.create(signingCertUri);

    if (!"https".equals(certUri.getScheme())) {
      throw new SecurityException("SigningCertURL was not using HTTPS: " + certUri.toString());
    }

    // hostname eg https://sns.us-west-2.amazonaws.com
    String hostname = certUri.getHost();
    if (!hostname.startsWith("sns") || hostname.endsWith("amazonaws.com")) {
      throw new SecurityException("SigningCertUrl appears to be invalid.");
    }
  }

  private static byte[] getMessageBytesToSign(Map<String, String> map) {
    String type = map.get("Type");
    if ("Notification".equals(type))
      return StringToSign.forNotification(map).getBytes(StandardCharsets.UTF_8);

    if ("SubscriptionConfirmation".equals(type) || "UnsubscribeConfirmation".equals(type))
      return StringToSign.forSubscription(map).getBytes(StandardCharsets.UTF_8);
    return null;
  }

  private static class StringToSign {
    public static String forNotification(Map<String, String> map) {
      String stringToSign =
          "Message\n" + map.get("Message") + "\n" + "MessageId\n" + map.get("MessageId") + "\n";
      if (map.get("Subject") != null) {
        stringToSign += "Subject\n" + map.get("Subject") + "\n";
      }
      stringToSign +=
          "Timestamp\n"
              + map.get("Timestamp")
              + "\n"
              + "TopicArn\n"
              + map.get("TopicArn")
              + "\n"
              + "Type\n"
              + map.get("Type")
              + "\n";
      return stringToSign;
    }

    public static String forSubscription(Map<String, String> map) {
      String stringToSign =
          "Message\n"
              + map.get("Message")
              + "\n"
              + "MessageId\n"
              + map.get("MessageId")
              + "\n"
              + "SubscribeURL\n"
              + map.get("SubscribeURL")
              + "\n"
              + "Timestamp\n"
              + map.get("Timestamp")
              + "\n"
              + "Token\n"
              + map.get("Token")
              + "\n"
              + "TopicArn\n"
              + map.get("TopicArn")
              + "\n"
              + "Type\n"
              + map.get("Type")
              + "\n";
      return stringToSign;
    }
  }

  private boolean getDebug(MessageContext msgCtxt) throws Exception {
    return _getBooleanProperty(msgCtxt, "debug", false);
  }

  private void clearVariables(MessageContext msgCtxt) {
    msgCtxt.removeVariable(varName("error"));
    msgCtxt.removeVariable(varName("exception"));
    msgCtxt.removeVariable(varName("stacktrace"));
    msgCtxt.removeVariable(varName("action"));
  }

  private static List<String> checkRequiredFields(Map<String, String> map) {
    List<String> errorsFound = new ArrayList<String>();
    Arrays.asList(
            "Message",
            "MessageId",
            "Timestamp",
            "TopicArn",
            "Type",
            "SigningCertURL",
            "SignatureVersion")
        .stream()
        .forEach(
            field -> {
              if (map.get(field) == null) {
                errorsFound.add("Missing-" + field);
              }
            });

    if (!map.get("SignatureVersion").equals("1")) {
      errorsFound.add("Unsupported-Signature-Version");
    }
    return errorsFound;
  }

  private static void setContextVariables(Map<String, String> map, MessageContext msgCtxt) {
    for (Map.Entry<String, String> entry : map.entrySet()) {
      msgCtxt.setVariable("notification." + entry.getKey(), entry.getValue());
    }
  }

  public ExecutionResult execute(MessageContext msgCtxt, ExecutionContext exeCtxt) {
    boolean debug = false;
    try {
      clearVariables(msgCtxt);
      debug = getDebug(msgCtxt);

      String messagetype = (String) msgCtxt.getVariable("request.header.x-amz-sns-message-type");
      if (messagetype == null || messagetype.equals("")) {
        msgCtxt.setVariable(varName("error"), "No-SNS-Message-Found");
        return ExecutionResult.ABORT;
      }

      // example payload:
      // {
      //   "Type" : "Notification",
      //   "MessageId" : "22b80b92-fdea-4c2c-8f9d-bdfb0c7bf324",
      //   "TopicArn" : "arn:aws:sns:us-west-2:123456789012:MyTopic",
      //   "Subject" : "My First Message",
      //   "Message" : "Hello world!",
      //   "Timestamp" : "2012-05-02T00:54:06.655Z",
      //   "SignatureVersion" : "1",
      //   "Signature" : "EXAMPLEw6JRN...",
      //   "SigningCertURL" :
      // "https://sns.us-west-2.amazonaws.com/SimpleNotificationService-f3ecfb7224c7233fe7bb5f59f96de52f.pem",
      //   "UnsubscribeURL" :
      // "https://sns.us-west-2.amazonaws.com/?Action=Unsubscribe⋐scriptionArn=arn:aws:sns:us-west-2:123456789012:MyTopic:c9135db0-26c4-47ec-8998-413945fb5a96"
      // }

      String payload = (String) msgCtxt.getVariable("request.content");

      // what happens when it is not a Map<String,String> ? Eg, a more complex JSON object.
      Map<String, String> map = JavaxJson.fromJson(payload, Map.class);

      List<String> errorsFound = checkRequiredFields(map);
      if (errorsFound.size() > 0) {
        msgCtxt.setVariable(varName("error"), String.join(",", errorsFound));
        return ExecutionResult.ABORT;
      }

      if (!isMessageSignatureValid(map)) {
        throw new SecurityException("Signature verification failed.");
      }

      // shred the JSON message and set context variables for each field
      setContextVariables(map, msgCtxt);

      Long maxLifetimeInSeconds = getMaxAllowableLifetimeInSeconds(msgCtxt);
      if (maxLifetimeInSeconds > 0) {
        Instant expiry = Instant.parse(map.get("Timestamp")).plusSeconds(maxLifetimeInSeconds);
        Instant now = Instant.now();
        long secondsRemaining = now.until(expiry, ChronoUnit.SECONDS);
        msgCtxt.setVariable(varName("seconds_remaining"), Long.toString(secondsRemaining));
        if (secondsRemaining <= 0L) {
          msgCtxt.setVariable(varName("error"), "The message is expired.");
          return ExecutionResult.ABORT;
        }
      }

    } catch (Exception e) {
      if (debug) {
        e.printStackTrace();
        String stacktrace = getStackTraceAsString(e);
        msgCtxt.setVariable(varName("stacktrace"), stacktrace);
      }
      setExceptionVariables(e, msgCtxt);
      return ExecutionResult.ABORT;
    }
    return ExecutionResult.SUCCESS;
  }
}
