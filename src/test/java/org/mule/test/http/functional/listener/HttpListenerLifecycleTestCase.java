/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.http.functional.listener;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.http.AllureConstants.HttpFeature.HTTP_EXTENSION;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.http.functional.AbstractHttpTestCase;

import java.io.IOException;
import java.net.ConnectException;

import io.qameta.allure.Feature;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(HTTP_EXTENSION)
public class HttpListenerLifecycleTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");
  @Rule
  public DynamicPort port2 = new DynamicPort("port2");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "http-listener-lifecycle-config.xml";
  }

  @Test
  public void stopOneListenerDoesNotAffectAnother() throws Exception {
    Lifecycle httpListener = (Lifecycle) ((Flow) getFlowConstruct("testPathFlow")).getSource();
    httpListener.stop();
    callAndAssertResponseFromUnaffectedListener(getLifecycleConfigUrl("/path/catch"), "catchAll");
    httpListener.start();
  }

  @Test
  public void restartListener() throws Exception {
    Lifecycle httpListener = (Lifecycle) ((Flow) getFlowConstruct("testPathFlow")).getSource();
    httpListener.stop();
    httpListener.start();
    final Response response = Request.Get(getLifecycleConfigUrl("/path/subpath")).execute();
    final HttpResponse httpResponse = response.returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is("ok"));
  }

  @Test
  public void stopListenerReturns404() throws Exception {
    Stoppable httpListener = (Stoppable) ((Flow) getFlowConstruct("catchAllWithinTestPathFlow")).getSource();
    httpListener.stop();
    final Response response = Request.Get(getLifecycleConfigUrl("/path/somepath")).execute();
    final HttpResponse httpResponse = response.returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(404));
  }

  @Test
  public void stoppedListenerConfigDoNotListen() throws Exception {
    Lifecycle httpListenerConfig = muleContext.getRegistry().get("testLifecycleListenerConfig");
    httpListenerConfig.stop();
    try {
      expectedException.expect(ConnectException.class);
      Request.Get(getLifecycleConfigUrl("/path/subpath")).execute();
    } finally {
      httpListenerConfig.start();
    }
  }

  @Test
  public void stopOneListenerConfigDoesNotAffectAnother() throws Exception {
    Lifecycle httpListenerConfig = muleContext.getRegistry().get("testLifecycleListenerConfig");
    httpListenerConfig.stop();
    callAndAssertResponseFromUnaffectedListener(getUnchangedConfigUrl(), "works");
    httpListenerConfig.start();
  }

  @Test
  public void restartListenerConfig() throws Exception {
    Lifecycle httpListenerConfig = muleContext.getRegistry().get("testLifecycleListenerConfig");
    httpListenerConfig.stop();
    httpListenerConfig.start();
    final Response response = Request.Get(getLifecycleConfigUrl("/path/anotherPath")).execute();
    final HttpResponse httpResponse = response.returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is("catchAll"));
  }

  private void callAndAssertResponseFromUnaffectedListener(String url, String expectedResponse) throws IOException {
    final Response response = Request.Get(url).execute();
    final HttpResponse httpResponse = response.returnResponse();
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
    assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(expectedResponse));
  }

  private String getLifecycleConfigUrl(String path) {
    return String.format("http://localhost:%s/%s", port1.getNumber(), path);
  }

  private String getUnchangedConfigUrl() {
    return String.format("http://localhost:%s/%s", port2.getNumber(), "/path");
  }

}