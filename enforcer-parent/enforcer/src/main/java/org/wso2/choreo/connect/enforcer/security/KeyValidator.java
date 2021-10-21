/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.choreo.connect.enforcer.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.wso2.choreo.connect.enforcer.commons.model.ResourceConfig;
import org.wso2.choreo.connect.enforcer.constants.APIConstants;
import org.wso2.choreo.connect.enforcer.constants.GeneralErrorCodeConstants;
import org.wso2.choreo.connect.enforcer.dto.APIKeyValidationInfoDTO;
import org.wso2.choreo.connect.enforcer.exception.EnforcerException;
import org.wso2.choreo.connect.enforcer.models.API;
import org.wso2.choreo.connect.enforcer.models.ApiPolicy;
import org.wso2.choreo.connect.enforcer.models.Application;
import org.wso2.choreo.connect.enforcer.models.ApplicationKeyMapping;
import org.wso2.choreo.connect.enforcer.models.ApplicationPolicy;
import org.wso2.choreo.connect.enforcer.models.Subscription;
import org.wso2.choreo.connect.enforcer.models.SubscriptionPolicy;
import org.wso2.choreo.connect.enforcer.models.URLMapping;
import org.wso2.choreo.connect.enforcer.subscription.SubscriptionDataHolder;
import org.wso2.choreo.connect.enforcer.subscription.SubscriptionDataStore;
import org.wso2.choreo.connect.enforcer.util.FilterUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Does the subscription and scope validation.
 */
public class KeyValidator {

    private static final Logger log = LogManager.getLogger(KeyValidator.class);

    public APIKeyValidationInfoDTO validateSubscription(String uuid, String apiContext, String apiVersion,
                                                        String consumerKey, String keyManager) {
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = new APIKeyValidationInfoDTO();

        try {
            if (log.isDebugEnabled()) {
                log.debug("Before validating subscriptions");
                log.debug("Validation Info : { uuid : " + uuid + " , context : " + apiContext +
                        " , version : " + apiVersion + " , consumerKey : " + consumerKey + " }");
            }
            validateSubscriptionDetails(uuid, apiContext, apiVersion, consumerKey, keyManager, apiKeyValidationInfoDTO);
            if (log.isDebugEnabled()) {
                log.debug("After validating subscriptions");
            }
        } catch (EnforcerException e) {
            log.error("Error Occurred while validating subscription.", e);
        }
        return apiKeyValidationInfoDTO;
    }

    public boolean validateScopes(TokenValidationContext validationContext) throws EnforcerException {

        if (validationContext.isCacheHit()) {
            return true;
        }
        APIKeyValidationInfoDTO apiKeyValidationInfoDTO = validationContext.getValidationInfoDTO();

        if (apiKeyValidationInfoDTO == null) {
            throw new EnforcerException("Key Validation information not set");
        }
        String[] scopes;
        Set<String> scopesSet = apiKeyValidationInfoDTO.getScopes();
        StringBuilder scopeList = new StringBuilder();

        if (scopesSet != null && !scopesSet.isEmpty()) {
            scopes = scopesSet.toArray(new String[scopesSet.size()]);
            if (log.isDebugEnabled() && scopes != null) {
                for (String scope : scopes) {
                    scopeList.append(scope);
                    scopeList.append(",");
                }
                scopeList.deleteCharAt(scopeList.length() - 1);
                log.debug("Scopes allowed for token : " + validationContext.getAccessToken() + " : " + scopeList
                        .toString());
            }
        }

        ResourceConfig matchedResource = validationContext.getMatchingResourceConfig();
        boolean scopesValidated = false;
        if (matchedResource.getSecuritySchemas().entrySet().size() > 0) {
            for (Map.Entry<String, List<String>> pair : matchedResource.getSecuritySchemas().entrySet()) {
                boolean validate = false;
                if (pair.getValue() != null && pair.getValue().size() > 0) {
                    scopesValidated = false;
                    for (String scope : pair.getValue()) {
                        if (scopesSet.contains(scope)) {
                            scopesValidated = true;
                            validate = true;
                            break;
                        }
                    }
                } else {
                    scopesValidated = true;
                }
                if (validate) {
                    break;
                }
            }
        } else {
            scopesValidated = true;
        }
        if (!scopesValidated) {
            apiKeyValidationInfoDTO.setAuthorized(false);
            apiKeyValidationInfoDTO.setValidationStatus(APIConstants.KeyValidationStatus.INVALID_SCOPE);
        }
        return scopesValidated;
    }

    private boolean validateSubscriptionDetails(String uuid, String context, String version, String consumerKey,
            String keyManager, APIKeyValidationInfoDTO infoDTO) throws EnforcerException {
        boolean defaultVersionInvoked = false;
        String apiTenantDomain = FilterUtils.getTenantDomainFromRequestURL(context);
        if (apiTenantDomain == null) {
            apiTenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;
        }
        // Check if the api version has been prefixed with _default_
        if (version != null && version.startsWith(APIConstants.DEFAULT_VERSION_PREFIX)) {
            defaultVersionInvoked = true;
            // Remove the prefix from the version.
            version = version.split(APIConstants.DEFAULT_VERSION_PREFIX)[1];
        }

        validateSubscriptionDetails(infoDTO, uuid, context, version, consumerKey, keyManager, defaultVersionInvoked);
        return infoDTO.isAuthorized();
    }

    private APIKeyValidationInfoDTO validateSubscriptionDetails(APIKeyValidationInfoDTO infoDTO, String uuid,
            String context, String version, String consumerKey, String keyManager, boolean defaultVersionInvoked) {
        String apiTenantDomain = FilterUtils.getTenantDomainFromRequestURL(context);
        if (apiTenantDomain == null) {
            apiTenantDomain = APIConstants.SUPER_TENANT_DOMAIN_NAME;
        }
        int tenantId = -1234; //TODO: get the correct tenant domain.
        API api = null;
        ApplicationKeyMapping key = null;
        Application app = null;
        Subscription sub = null;

        SubscriptionDataStore datastore = SubscriptionDataHolder.getInstance()
                .getTenantSubscriptionStore(apiTenantDomain);
        //TODO add a check to see whether datastore is initialized an load data using rest api if it is not loaded
        // TODO: (VirajSalaka) Handle the scenario where the event is dropped.
        if (datastore != null) {
            api = datastore.getApiByContextAndVersion(uuid);
            if (api != null) {
                key = datastore.getKeyMappingByKeyAndKeyManager(consumerKey, keyManager);
                if (key != null) {
                    app = datastore.getApplicationById(key.getApplicationUUID());
                    if (app != null) {
                        sub = datastore.getSubscriptionById(app.getUUID(), api.getApiUUID());
                        if (sub != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("All information is retrieved from the inmemory data store.");
                            }
                        } else {
                            log.info(
                                    "Valid subscription not found for application : " + app.getName()
                                            + " with app uuid : " + app.getUUID() + " and api : " + api.getApiName()
                                            + " with api uuid : " + api.getApiUUID());
                        }
                    } else {
                        log.info("Application not found in the data store for uuid " + key.getApplicationUUID());
                    }
                } else {
                    log.info("Application key mapping not found in the data store for id consumerKey " + consumerKey);
                }
            } else {
                log.info("API not found in the data store for API UUID :" + uuid);
            }
        } else {
            log.error("Subscription data store is null for tenant domain " + apiTenantDomain);
        }

        if (api != null && app != null && key != null && sub != null) {
            validate(infoDTO, apiTenantDomain, tenantId, datastore, api, key, app, sub, keyManager);
        } else if (!infoDTO.isAuthorized() && infoDTO.getValidationStatus() == 0) {
            //Scenario where validation failed and message is not set
            infoDTO.setValidationStatus(APIConstants.KeyValidationStatus.API_AUTH_RESOURCE_FORBIDDEN);
        }

        return infoDTO;
    }

    private APIKeyValidationInfoDTO validate(APIKeyValidationInfoDTO infoDTO, String apiTenantDomain, int tenantId,
            SubscriptionDataStore datastore, API api, ApplicationKeyMapping key, Application app, Subscription sub,
            String keyManager) {
        String subscriptionStatus = sub.getSubscriptionState();
        String type = key.getKeyType();
        if (APIConstants.SubscriptionStatus.BLOCKED.equals(subscriptionStatus)) {
            infoDTO.setValidationStatus(APIConstants.KeyValidationStatus.API_BLOCKED);
            infoDTO.setAuthorized(false);
            return infoDTO;
        } else if (APIConstants.SubscriptionStatus.ON_HOLD.equals(subscriptionStatus)
                || APIConstants.SubscriptionStatus.REJECTED.equals(subscriptionStatus)) {
            infoDTO.setValidationStatus(APIConstants.KeyValidationStatus.SUBSCRIPTION_INACTIVE);
            infoDTO.setAuthorized(false);
            return infoDTO;
        } else if (APIConstants.SubscriptionStatus.PROD_ONLY_BLOCKED.equals(subscriptionStatus)
                && !APIConstants.API_KEY_TYPE_SANDBOX.equals(type)) {
            infoDTO.setValidationStatus(APIConstants.KeyValidationStatus.API_BLOCKED);
            infoDTO.setType(type);
            infoDTO.setAuthorized(false);
            return infoDTO;
        } else if (APIConstants.LifecycleStatus.BLOCKED.equals(api.getLcState())) {
            infoDTO.setValidationStatus(GeneralErrorCodeConstants.API_BLOCKED_CODE);
            infoDTO.setAuthorized(false);
            return infoDTO;
        }
        infoDTO.setTier(sub.getPolicyId());
        infoDTO.setSubscriber(app.getSubName());
        infoDTO.setApplicationId(app.getUUID());
        infoDTO.setApiName(api.getApiName());
        infoDTO.setApiVersion(api.getApiVersion());
        infoDTO.setApiPublisher(api.getApiProvider());
        infoDTO.setApplicationName(app.getName());
        infoDTO.setApplicationTier(app.getPolicy());
        infoDTO.setApplicationUUID(app.getUUID());
        infoDTO.setAppAttributes(app.getAttributes());
        infoDTO.setApiUUID(api.getApiUUID());
        infoDTO.setType(type);
        infoDTO.setSubscriberTenantDomain(app.getTenantDomain());
        // Advanced Level Throttling Related Properties
        String apiTier = api.getApiTier();

        ApplicationPolicy appPolicy = datastore.getApplicationPolicyByName(app.getPolicy());
        SubscriptionPolicy subPolicy = datastore.getSubscriptionPolicyByName(sub.getPolicyId());
        ApiPolicy apiPolicy = datastore.getApiPolicyByName(api.getApiTier());
        boolean isContentAware = false;
        if (appPolicy.isContentAware() || subPolicy.isContentAware() || (apiPolicy != null && apiPolicy
                .isContentAware())) {
            isContentAware = true;
        }
        infoDTO.setContentAware(isContentAware);

        // TODO this must implement as a part of throttling implementation.
        int spikeArrest = 0;
        String apiLevelThrottlingKey = "api_level_throttling_key";

        if (subPolicy.getRateLimitCount() > 0) {
            spikeArrest = subPolicy.getRateLimitCount();
        }

        String spikeArrestUnit = null;

        if (subPolicy.getRateLimitTimeUnit() != null) {
            spikeArrestUnit = subPolicy.getRateLimitTimeUnit();
        }
        boolean stopOnQuotaReach = subPolicy.isStopOnQuotaReach();
        int graphQLMaxDepth = 0;
        if (subPolicy.getGraphQLMaxDepth() > 0) {
            graphQLMaxDepth = subPolicy.getGraphQLMaxDepth();
        }
        int graphQLMaxComplexity = 0;
        if (subPolicy.getGraphQLMaxComplexity() > 0) {
            graphQLMaxComplexity = subPolicy.getGraphQLMaxComplexity();
        }
        List<String> list = new ArrayList<String>();
        list.add(apiLevelThrottlingKey);
        infoDTO.setSpikeArrestLimit(spikeArrest);
        infoDTO.setSpikeArrestUnit(spikeArrestUnit);
        infoDTO.setStopOnQuotaReach(stopOnQuotaReach);
        infoDTO.setGraphQLMaxDepth(graphQLMaxDepth);
        infoDTO.setGraphQLMaxComplexity(graphQLMaxComplexity);
        if (apiTier != null && apiTier.trim().length() > 0) {
            infoDTO.setApiTier(apiTier);
        }
        // We also need to set throttling data list associated with given API. This need to have
        // policy id and
        // condition id list for all throttling tiers associated with this API.
        infoDTO.setThrottlingDataList(list);
        infoDTO.setAuthorized(true);
        return infoDTO;
    }

    private boolean isResourcePathMatching(String resourceString, URLMapping urlMapping) {

        String resource = resourceString.trim();
        String urlPattern = urlMapping.getUrlPattern().trim();

        if (resource.equalsIgnoreCase(urlPattern)) {
            return true;
        }

        // If the urlPattern is only one character longer than the resource and the urlPattern ends with a '/'
        if (resource.length() + 1 == urlPattern.length() && urlPattern.endsWith("/")) {
            // Check if resource is equal to urlPattern if the trailing '/' of the urlPattern is ignored
            String urlPatternWithoutSlash = urlPattern.substring(0, urlPattern.length() - 1);
            return resource.equalsIgnoreCase(urlPatternWithoutSlash);
        }

        return false;
    }
}
