# AWS SNS Verifier sample proxy bundle

This directory contains the configuration for a sample proxy bundle
that shows how to use the Java custom policy for doing Verification of AWS SNS messages.

## Using the Proxy

Import and deploy the Proxy to your favorite Edge organization + environment.

## Verify

To verify, specify the endpoint to the proxy, as the the SNS webhook URL.

```
$endpoint=https://${ORG}-${ENV}.apigee.net

# specify as SNS Webhook:
$endpoint/aws-sns-verifier/verify

```

## Bugs

None?
