/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org).
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.tests.testCases.standalone.interceptor;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.choreo.connect.mockbackend.InterceptorConstants;
import org.wso2.choreo.connect.tests.util.HttpClientRequest;
import org.wso2.choreo.connect.tests.util.HttpResponse;
import org.wso2.choreo.connect.tests.util.HttpsClientRequest;
import org.wso2.choreo.connect.tests.util.TestConstant;
import org.wso2.choreo.connect.tests.util.TokenUtil;
import org.wso2.choreo.connect.tests.util.Utils;

import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterceptorTestcase {
    protected String jwtTokenProd;

    private static final String INVOCATION_CONTEXT = "invocationContext";

    @BeforeClass(description = "initialise the setup")
    void start() throws Exception {
        jwtTokenProd = TokenUtil.getJwtForPetstore(TestConstant.KEY_TYPE_PRODUCTION, null, false);
    }

    @DataProvider(name = "requestBodyProvider")
    Object[][] requestBodyProvider() {
        String clientReqBody = "{\"name\": \"foo\", \"age\": 16}";
        String interceptorRespBody = "<student><name>Foo</name><age type=\"Y\">16</age></student>";

        // {clientReqBody, interceptorRespBody, isOmitInterceptorRespBody, reqToBackend}
        return new Object[][]{
                // non empty body from interceptor - means update request to backend
                {clientReqBody, interceptorRespBody, false, interceptorRespBody},
                // empty response body from interceptor - means update request to backend as empty
                {clientReqBody, "", false, ""},
                // null response body from interceptor (i.e. {"body": null}) - means do not update request to backend
                {clientReqBody, null, false, clientReqBody},
                // no response from interceptor (i.e. {}) - means do not update request to backend
                {clientReqBody, null, true, clientReqBody}
        };
    }

    @DataProvider(name = "directRespondRequestBodyProvider")
    Object[][] directRespondRequestBodyProvider() {
        String clientReqBody = "{\"name\": \"foo\", \"age\": 16}";
        String interceptorRespBody = "{\"message\": \"This is direct responded\"}";

        // {clientReqBody, interceptorRespBody, isOmitInterceptorRespBody, clientRespBody}
        return new Object[][]{
                // non empty body from interceptor - means update request to backend
                {clientReqBody, interceptorRespBody, false, interceptorRespBody},
                // empty response body from interceptor - means update request to backend as empty
                {clientReqBody, "", false, ""},
                // null response body from interceptor (i.e. {"body": null}) - means do not update request to backend
                {clientReqBody, null, false, ""},
                // no response from interceptor (i.e. {}) - means do not update request to backend
                {clientReqBody, null, true, ""}
        };
    }


    @BeforeMethod(description = "clear the status of interceptor management service")
    void clearInterceptorStatus() throws Exception {
        HttpClientRequest.doGet(Utils.getMockInterceptorManagerHttp("/interceptor/clear-status"));
    }

    @Test(description = "Test request body to interceptor service in request flow")
    public void testRequestToInterceptorServiceInRequestFlowInterception() throws Exception {
        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put("foo-remove", "Header_to_be_deleted");
        headers.put("foo-update", "Header_to_be_updated");
        headers.put("foo-keep", "Header_to_be_kept");
        headers.put("content-type", "application/xml");
        String body = "<student><name>Foo</name><age type=\"Y\">16</age></student>";
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                "/intercept-request/echo/123"), body, headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");

        // check which flows are invoked in interceptor service
        JSONObject status = new JSONObject(getInterceptorStatus());
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        testInterceptorHandler(handler, InterceptorConstants.Handler.REQUEST_ONLY);

        JSONObject reqFlowBodyJSON = new JSONObject(status.getString(InterceptorConstants.StatusPayload.REQUEST_FLOW_REQUEST_BODY));
        // invocation context
        // TODO: (renuka) change lua script: method -> supportedMethods and add current method from headers
        testInvocationContext(reqFlowBodyJSON, Arrays.asList("GET", "POST"), "POST", "/intercept-request/echo/123", "/echo/{id}");
        // headers
        headers.remove(HttpHeaderNames.AUTHORIZATION.toString()); // check without auth header
        testInterceptorHeaders(reqFlowBodyJSON, headers, true);
        // body
        testInterceptorBody(reqFlowBodyJSON, body, true);
    }

    @Test(
            description = "Test request body and headers to backend service with request flow interception",
            dataProvider = "requestBodyProvider"
    )
    public void testRequestToBackendServiceInRequestFlowInterception(
            String clientReqBody, String interceptorRespBody, boolean isOmitInterceptorRespBody, String reqToBackend)
            throws Exception {

        // JSON request to XML backend
        // setting response body of interceptor service
        JSONObject interceptorRespBodyJSON = new JSONObject();
        if (!isOmitInterceptorRespBody) {
            if (interceptorRespBody != null) {
                interceptorRespBodyJSON.put("body", Base64.getEncoder().encodeToString(interceptorRespBody.getBytes()));
            } else {
                interceptorRespBodyJSON.put("body", (String) null);
            }
        }
        interceptorRespBodyJSON.put("headersToAdd", Collections.singletonMap("foo-add", "Header_newly_added"));
        Map<String, String> headersToReplace = new HashMap<>();
        headersToReplace.put("foo-update", "Header_Updated");
        headersToReplace.put("foo-update-not-exist", "Header_Updated_New_Val");
        headersToReplace.put("content-type", "application/xml");
        interceptorRespBodyJSON.put("headersToReplace", headersToReplace);
        interceptorRespBodyJSON.put("headersToRemove", Collections.singletonList("foo-remove"));
        setResponseOfInterceptor(interceptorRespBodyJSON.toString(), true);

        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put("foo-remove", "Header_to_be_deleted");
        headers.put("foo-update", "Header_to_be_updated");
        headers.put("foo-keep", "Header_to_be_kept");
        headers.put("content-type", "application/json");
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                "/intercept-request/echo/123"), clientReqBody, headers);

        Assert.assertNotNull(response);
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");

        // check which flows are invoked in interceptor service
        JSONObject status = new JSONObject(getInterceptorStatus());
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        testInterceptorHandler(handler, InterceptorConstants.Handler.REQUEST_ONLY);

        // test headers
        JSONObject backendResponse = new JSONObject(response.getData());
        JSONObject respHeaders = backendResponse.getJSONObject("headers"); // headers key are capitalized from echo server
        Assert.assertFalse(respHeaders.has("Foo-remove"), "Failed to remove header");
        Assert.assertEquals(respHeaders.getJSONArray("Foo-add").getString(0), "Header_newly_added",
                "Failed to add new header");
        Assert.assertEquals(respHeaders.getJSONArray("Foo-update").getString(0), "Header_Updated",
                "Failed to replace header");
        Assert.assertEquals(respHeaders.getJSONArray("Foo-update-not-exist").getString(0), "Header_Updated_New_Val",
                "Failed to replace header");
        Assert.assertEquals(respHeaders.getJSONArray("Content-type").getString(0), "application/xml",
                "Failed to replace header");
        Assert.assertEquals(respHeaders.getJSONArray("Foo-keep").getString(0), "Header_to_be_kept",
                "Failed to keep original header");
        // test body
        Assert.assertEquals(backendResponse.getString("body"), reqToBackend);
    }

    @Test(
            description = "Direct respond when response interception is enabled",
            dataProvider = "directRespondRequestBodyProvider"
    )
    public void directRespondWhenResponseInterceptionEnabled(
            String clientReqBody, String interceptorRespBody, boolean isOmitInterceptorRespBody, String clientRespBody)
            throws Exception {

        // setting response body of interceptor service
        JSONObject interceptorRespBodyJSON = new JSONObject();
        interceptorRespBodyJSON.put("directRespond", true);
        if (!isOmitInterceptorRespBody) {
            if (interceptorRespBody != null) {
                interceptorRespBodyJSON.put("body", Base64.getEncoder().encodeToString(interceptorRespBody.getBytes()));
            } else {
                interceptorRespBodyJSON.put("body", (String) null);
            }
        }
        // only headersToAdd is considered when direct respond
        Map<String, String> headersToAdd = new HashMap<>();
        headersToAdd.put("foo-add", "Header_newly_added");
        headersToAdd.put("content-type", "application/json");
        interceptorRespBodyJSON.put("headersToAdd", headersToAdd);
        interceptorRespBodyJSON.put("headersToReplace", Collections.singletonMap("foo-ignored", "Header_not_added"));
        setResponseOfInterceptor(interceptorRespBodyJSON.toString(), true);

        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + jwtTokenProd);
        headers.put("foo-client-header", "Header_discard_when_respond");
        headers.put("content-type", "application/json");
        // this is not an echo server, so if request goes to backend it will respond with different payload.
        HttpResponse response = HttpsClientRequest.doPost(Utils.getServiceURLHttps(
                "/intercept-request/pet/findByStatus/resp-intercept-enabled"), clientReqBody, headers);

        Assert.assertNotNull(response);
        if (StringUtils.isEmpty(clientRespBody)) {
            Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_NO_CONTENT, "Response code mismatched");
        } else {
            Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        }

        // check which flows are invoked in interceptor service
        JSONObject status = new JSONObject(getInterceptorStatus());
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        testInterceptorHandler(handler, InterceptorConstants.Handler.REQUEST_ONLY);

        // test headers
        Assert.assertFalse(response.getHeaders().containsKey("foo-client-header"), "Responding client headers back");
        Assert.assertFalse(response.getHeaders().containsKey("foo-ignored"), "Should only support add headers");
        Assert.assertEquals(response.getHeaders().get("foo-add"), "Header_newly_added",
                "Failed to add new header");
        Assert.assertEquals(response.getHeaders().get("content-type"), "application/json",
                "Failed to replace header");
        // test body
        Assert.assertEquals(response.getData(), clientRespBody);
    }

    @Test(description = "Enforcer denied request when response interceptor enabled")
    public void enforcerDeniedRequestWhenResponseInterceptionEnabled() throws Exception {
        // setting client
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer INVALID-XXX-XXX-XXX-XXX-XXX-XXX-TOKEN");
        HttpResponse response = HttpsClientRequest.doGet(Utils.getServiceURLHttps(
                "/intercept-request/pet/findByStatus/resp-intercept-enabled"), headers);
        Assert.assertNotNull(response);

        // check which flows are invoked in interceptor service
        JSONObject status = new JSONObject(getInterceptorStatus());
        String handler = status.getString(InterceptorConstants.StatusPayload.HANDLER);
        testInterceptorHandler(handler, InterceptorConstants.Handler.NONE); // no interceptor handle the request
    }

    private String getInterceptorStatus() throws Exception {
        HttpResponse response = HttpClientRequest.doGet(Utils.getMockInterceptorManagerHttp("/interceptor/status"));
        Assert.assertNotNull(response, "Invalid response from interceptor status");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
        return response.getData();
    }

    private void setResponseOfInterceptor(String responseBody, boolean isRequestFlow) throws Exception {
        String servicePath = isRequestFlow ? "interceptor/request" : "interceptor/response";
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json");
        HttpResponse response = HttpClientRequest.doPost(Utils.getMockInterceptorManagerHttp(servicePath),
                responseBody, headers);
        Assert.assertNotNull(response, "Invalid response when updating response body of interceptor");
        Assert.assertEquals(response.getResponseCode(), HttpStatus.SC_OK, "Response code mismatched");
    }

    private void testInterceptorHandler(String actualHandler, InterceptorConstants.Handler expectedHandler) {
        Assert.assertEquals(actualHandler, expectedHandler.toString(), "Invalid interceptor handler");
    }

    private void testInvocationContext(JSONObject bodyJSON, List<String> supportedMethods, String method, String path, String pathTemplate) {
        JSONObject invocationCtx = bodyJSON.getJSONObject(INVOCATION_CONTEXT);
        Assert.assertNotNull(invocationCtx, "Interceptor invocation context not found");

        Assert.assertEquals(invocationCtx.getString("apiName"), "SwaggerPetstoreRequestIntercept", "API name mismatch");
        Assert.assertEquals(invocationCtx.getString("apiVersion"), "1.0.5", "API version mismatch");
        Assert.assertEquals(invocationCtx.getString("scheme"), "https", "Scheme mismatch");
        Assert.assertEquals(invocationCtx.getString("vhost"), "localhost", "Vhost mismatch");
//        Assert.assertEquals(invocationCtx.getString("method"), method, "HTTP method mismatch");
        Assert.assertEquals(invocationCtx.getString("path"), path, "Resource path mismatch");
        Assert.assertEquals(invocationCtx.getString("pathTemplate"), pathTemplate, "Resource path template mismatch");
        Assert.assertEquals(invocationCtx.getString("basePath"), "/intercept-request", "Base path mismatch");

        Assert.assertTrue(StringUtils.isNotEmpty(invocationCtx.getString("source")), "Source not found");
        Assert.assertTrue(StringUtils.isNotEmpty(invocationCtx.getString("requestId")), "Request ID not found");
        Assert.assertEquals(Arrays.asList(invocationCtx.getString("method").split(" ")), supportedMethods, "HTTP supported method mismatch"); // TODO: change this
    }

    private void testInterceptorHeaders(JSONObject bodyJSON, Map<String, String> expectedHeaders, boolean isRequestFlow) {
        String jsonKey = isRequestFlow ? "requestHeaders" : "responseHeaders";
        JSONObject headersJSON = bodyJSON.getJSONObject(jsonKey);
        expectedHeaders.forEach((key, value) -> {
            String actualVal = headersJSON.getString(key);
            Assert.assertEquals(actualVal, value, String.format("Header mismatch for header key: %s", key));
        });
    }

    private void testInterceptorBody(JSONObject bodyJSON, String expectedBody, boolean isRequestFlow) {
        String jsonKey = isRequestFlow ? "requestBody" : "responseBody";
        String base64EncodedBody = Base64.getEncoder().encodeToString(expectedBody.getBytes());
        Assert.assertEquals(bodyJSON.getString(jsonKey), base64EncodedBody, "Request body mismatch");
    }
}
