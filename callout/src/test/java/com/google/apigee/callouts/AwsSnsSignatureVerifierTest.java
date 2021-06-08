// AwsSnsSignatureVerifierTest.java
//
// Test code for the AWS SNS Signature Verifier custom policy for Apigee. Uses TestNG.
// For full details see the Readme accompanying this source file.
//
// Copyright (c) 2021 Google LLC
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
import com.apigee.flow.message.MessageContext;
import java.util.HashMap;
import java.util.Map;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AwsSnsSignatureVerifierTest {

  static {
    java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
  }

  MessageContext msgCtxt;
  ExecutionContext exeCtxt;

  @BeforeMethod()
  public void testSetup1() {

    msgCtxt =
        new MockUp<MessageContext>() {
          private Map<String, Object> variables;

          public void $init() {
            getVariables();
          }

          private Map<String, Object> getVariables() {
            if (variables == null) {
              variables = new HashMap<String, Object>();
            }
            return variables;
          }

          @Mock()
          public Object getVariable(final String name) {
            return getVariables().get(name);
          }

          @Mock()
          public boolean setVariable(final String name, final Object value) {
            getVariables().put(name, value);
            return true;
          }

          @Mock()
          public boolean removeVariable(final String name) {
            if (getVariables().containsKey(name)) {
              variables.remove(name);
            }
            return true;
          }
        }.getMockInstance();

    exeCtxt = new MockUp<ExecutionContext>() {}.getMockInstance();
    System.out.printf("=============================================\n");
  }

  private void reportThings(Map<String, String> props) {
    String test = props.get("testname");
    System.out.println("test  : " + test);
    String cipher = msgCtxt.getVariable("crypto_cipher");
    System.out.println("cipher: " + cipher);
    String action = msgCtxt.getVariable("crypto_action");
    System.out.println("action: " + action);
    String outputEncoding = msgCtxt.getVariable("crypto_output_encoding");
    System.out.println("outputEncoding: " + outputEncoding);
    String output = msgCtxt.getVariable("crypto_output");
    System.out.println("output: " + output);
    String error = msgCtxt.getVariable("crypto_error");
    System.out.println("error : " + error);
  }

  // no test available

  // @Test()
  // public void basic() {
  //   Properties props = new Properties();
  //   // props.setProperty("debug", "true");
  //   props.setProperty("debug", "true");
  //   // props.setProperty("source", "source"); // no source!
  //   props.setProperty("request-verb", "GET");
  //   props.setProperty("request-path", "/test.txt");
  //   props.setProperty("request-date", "20130524T000000Z");
  //   props.setProperty("request-expiry", "86400");
  //   props.setProperty("output", "my_output");
  //
  //   props.setProperty("key", "AKIAIOSFODNN7EXAMPLE");
  //   props.setProperty("secret", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY");
  //   props.setProperty("region", "us-east-1");
  //   props.setProperty("service", "s3");
  //   props.setProperty("endpoint", "https://examplebucket.s3.amazonaws.com");
  //
  //   AwsSnsSignatureVerifier callout = new AwsSnsSignatureVerifier(props);
  //
  //   // execute and retrieve output
  //   ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
  //   ExecutionResult expectedResult = ExecutionResult.SUCCESS;
  //
  //   // check result and output
  //   Assert.assertEquals(actualResult, expectedResult, testName + " result not as expected");
  //   Assert.assertNull(msgCtxt.getVariable("awsv4sig_error"), testName);
  //   Assert.assertEquals(msgCtxt.getVariable("awsv4sig_creq"), creq, testName);
  //   Assert.assertEquals(stsXformed(), sts, testName);
  //   Assert.assertEquals(msgCtxt.getVariable("my_output"), constructedUrl, testName);
  //
  // }
}
