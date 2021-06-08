# AWS SNS Signature Verifier callout

This directory contains the Java source code for a Java callout for Apigee
that performs Verification of an AWS SNS Signature, version 1, as described in [the AWS SNS documentation](https://docs.aws.amazon.com/sns/latest/dg/sns-example-code-endpoint-java-servlet.html).
The signature is RSA signing with the SHA1 hash, using a specifically constructed string-to-sign.
This callout does not perform RSA signing, only verification.

## License

This code is Copyright (c) 2021 Google LLC, and is released under the
Apache Source License v2.0. For information see the [LICENSE](LICENSE) file.

## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.

## Using the Custom Policy

You do not need to build the Jar in order to use the custom policy.

When you use the policy to verify SNS signatures, you can be assured that the message was signed as-is by AWS SNS.
You can then trust the claims in the message.



## Policy Configuration

The policy verifies the inbound "request" object.

There is one option, `max-lifetime`, which you can use to set the message expiry.

## Example: Basic Verification with 60s expiry

  ```xml
  <JavaCallout name="Java-AWS-SNS-Verify">
    <Properties>
      <Property name='max-lifetime'>60s</Property>
    </Properties>
    <ClassName>com.google.apigee.callouts.AwsSnsSignatureVerifier</ClassName>
    <ResourceURL>java://apigee-callout-aws-sns-sig-verifier-202100608.jar</ResourceURL>
  </JavaCallout>
  ```

Here's what will happen with this policy configuration:

* Check that the request is from SNS (has the well-known header)
* Checks that the Signing Cert URL is hosted at amazonaws.com for SNS
* Builds the string-to-sign and verifies it against the certificate provided by the Signing Cert URL
* Verifies the signature against that string-to-sign
* Checks that the timestamp is no more than 60 seconds prior.


## Detecting Success and Errors

The policy will return ABORT and set the context variable `awssns_error` if there has been any error at runtime. Your proxy bundles can check this variable in `FaultRules`.

Errors can result at runtime if:

* the message is not from SNS
* the message is expired
* the message signature does not verify

## Building the Jar

You do not need to build the Jar in order to use the custom policy. The custom policy is
ready to use, with policy configuration. You need to re-build the jar only if you want
to modify the behavior of the custom policy. Before you do that, be sure you understand
all the configuration options - the policy may be usable for you without modification.

If you do wish to build the jar, you can use [maven](https://maven.apache.org/download.cgi) to do so. The build requires JDK8. Before you run the build the first time, you need to download the Apigee dependencies into your local maven repo.

Preparation, first time only: `./buildsetup.sh`

To build: `mvn clean package`

The Jar source code includes tests.

If you edit policies offline, copy [the jar file for the custom policy](callout/target/apigee-callout-aws-sns-sig-verifier-202100608.jar)  to your apiproxy/resources/java directory.  If you don't edit proxy bundles offline, upload that jar file into the API Proxy via the Apigee API Proxy Editor .


## Build Dependencies

* Apigee expressions v1.0
* Apigee message-flow v1.0
* Bouncy Castle 1.64
* Ben Manes' Caffeine 2.9.1

These jars are specified in the pom.xml file.

The first two JARs are builtin to Apigee. You will need to upload the
BouncyCastle jar as a resource to your Apigee instance, either
with the apiproxy or with the organization or environment.


## Author

Dino Chiesa
godino@google.com


## Bugs & Limitations

* ??
