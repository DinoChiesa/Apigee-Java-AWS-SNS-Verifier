// CertCacheTest.java
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

package com.google.apigee.util;

import java.security.cert.X509Certificate;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CertCacheTest {

  @Test()
  public void retrieveWithCache() throws Exception {

    // Clear the cache stats
    CertCache.getCacheStats().minus(CertCache.getCacheStats());

    int max = 100;
    int i = 0;
    while (i++ < max) {
      X509Certificate cert =
          CertCache.getCert("http://sns.us-east-1.amazonaws.com/SimpleNotificationService.pem");
      Assert.assertNotNull(cert);
      cert.checkValidity();
      Assert.assertNotNull(cert.getSubjectX500Principal());
      if (i == 1) {
        System.out.printf("issuer: %s\n", cert.getIssuerX500Principal().toString());
        System.out.printf("subject: %s\n", cert.getSubjectX500Principal().toString());
        System.out.printf("notBefore: %s\n", cert.getNotBefore().toString());
        System.out.printf("notAfter: %s\n", cert.getNotAfter().toString());
      }
    }

    // verify that the cache was hit.
    Assert.assertTrue(
        CertCache.getCacheStats().hitCount() >= (max - 1),
        String.format(
            "Expected at least %s hits. Stats are %s", max - 1, CertCache.getCacheStats()));
  }
}
