/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.http.functional;

import static org.mule.test.http.AllureConstants.HttpFeature.HTTP_EXTENSION;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.http.api.HttpService;
import org.mule.service.http.TestHttpClient;

import io.qameta.allure.Feature;
import org.junit.Rule;

@Feature(HTTP_EXTENSION)
public abstract class AbstractHttpTestCase extends MuleArtifactFunctionalTestCase {

  protected static final int DEFAULT_TIMEOUT = 1000;

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();
}
