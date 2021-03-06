/*
 * Copyright 2018 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.spark

import com.thoughtworks.go.api.mocks.MockHttpServletResponseAssert
import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.i18n.Localizer
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.http.mocks.HttpRequestBuilder
import com.thoughtworks.go.http.mocks.MockHttpServletRequest
import com.thoughtworks.go.http.mocks.MockHttpServletResponse
import com.thoughtworks.go.server.service.PipelineConfigService
import com.thoughtworks.go.server.util.UserHelper
import com.thoughtworks.go.spark.mocks.StubTemplateEngine
import com.thoughtworks.go.spark.mocks.TestApplication
import com.thoughtworks.go.spark.mocks.TestRequestContext
import com.thoughtworks.go.spark.mocks.TestSparkPreFilter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockito.invocation.InvocationOnMock
import spark.servlet.SparkFilter

import javax.servlet.Filter
import javax.servlet.FilterConfig

import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyVararg
import static org.mockito.Mockito.*

trait ControllerTrait<T extends SparkController> {

  private T _controller
  Filter prefilter
  MockHttpServletRequest request
  MockHttpServletResponse response
  RequestContext requestContext = new TestRequestContext()
  StubTemplateEngine templateEngine = new StubTemplateEngine()
  HttpRequestBuilder httpRequestBuilder = new HttpRequestBuilder();
  Localizer localizer = mock(Localizer.class)
  PipelineConfigService pipelineConfigService = mock(PipelineConfigService.class)

  void get(String path) {
    sendRequest('get', path, [:], null)
  }

  void get(String path, Map headers) {
    sendRequest('get', path, headers, null)
  }

  void put(String path, Object body) {
    sendRequest('put', path, ['content-type': 'application/json'], body)
  }

  void post(String path, Object body) {
    sendRequest('post', path, ['content-type': 'application/json'], body)
  }

  void patch(String path, Object body) {
    sendRequest('patch', path, ['content-type': 'application/json'], body)
  }

  void delete(String path) {
    sendRequest('delete', path, [:], null)
  }

  void getWithApiHeader(String path) {
    getWithApiHeader(path, [:])
  }

  void getWithApiHeader(String path, Map headers) {
    sendRequest('get', path, headers + ['accept': controller.mimeType], null)
  }

  void putWithApiHeader(String path, Object body) {
    putWithApiHeader(path, [:], body)
  }

  void putWithApiHeader(String path, Map headers, Object body) {
    sendRequest('put', path, headers + ['accept': controller.mimeType, 'content-type': 'application/json'], body)
  }

  void postWithApiHeader(String path, Object body) {
    postWithApiHeader(path, [:], body)
  }

  void postWithApiHeader(String path, Map headers, Object body) {
    sendRequest('post', path, headers + ['accept': controller.mimeType, 'content-type': 'application/json'], body)
  }

  void patchWithApiHeader(String path, Object body) {
    patchWithApiHeader(path, [:], body)
  }

  void patchWithApiHeader(String path, Map headers, Object body) {
    sendRequest('patch', path, headers + ['accept': controller.mimeType, 'content-type': 'application/json'], body)
  }

  void deleteWithApiHeader(String path) {
    deleteWithApiHeader(path, [:])
  }

  void deleteWithApiHeader(String path, Map headers) {
    sendRequest('delete', path, headers + ['accept': controller.mimeType], null)
  }

  void sendRequest(String httpVerb, String path, Map<String, String> headers, Object requestBody) {
    httpRequestBuilder.withPath(path).withMethod(httpVerb).withHeaders(headers)

    if (requestBody != null) {
      if (requestBody instanceof String) {
        httpRequestBuilder.withJsonBody((String) requestBody)
      } else {
        httpRequestBuilder.withJsonBody((Object) requestBody)
      }
    }

    if (!currentUsername().isAnonymous()) {
      httpRequestBuilder.withSessionAttr(UserHelper.getSessionKeyForUserId(), currentUserLoginId())
    }

    request = httpRequestBuilder.build()
    response = new MockHttpServletResponse()

    getPrefilter().doFilter(request, response, null)
  }

  private Filter getPrefilter() {
    if (prefilter == null) {
      def filterConfig = mock(FilterConfig.class)
      when(filterConfig.getInitParameter(SparkFilter.APPLICATION_CLASS_PARAM)).thenReturn(TestApplication.class.getName())
      prefilter = new TestSparkPreFilter(new TestApplication(controller))
      prefilter.init(filterConfig)
    }
    return prefilter
  }

  T getController() {
    if (_controller == null) {
      _controller = spy(createControllerInstance())
    }
    return _controller
  }

  MockHttpServletResponseAssert assertThatResponse() {
    MockHttpServletResponseAssert.assertThat(response)
  }

  abstract T createControllerInstance()

  @BeforeEach
  void setupLocalizer() {
    when(localizer.localize(any() as String, anyVararg())).then({ InvocationOnMock invocation ->
      return invocation.getArguments().first()
    })
  }

  @BeforeEach
  @AfterEach
  void clearSingletons() {
    ClearSingleton.clearSingletons()
  }

  @AfterEach
  void destroyPrefilter() {
    getPrefilter().destroy()
  }

  Username currentUsername() {
    return UserHelper.getUserName()
  }

  CaseInsensitiveString currentUserLoginName() {
    return currentUsername().getUsername()
  }

  Long currentUserLoginId() {
    if (currentUsername().isAnonymous()) {
      return null
    }

    currentUsername().hashCode()
  }
}
