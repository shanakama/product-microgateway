package xds

import (
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/wso2/product-microgateway/adapter/config"
	logger "github.com/wso2/product-microgateway/adapter/internal/loggers"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/config/enforcer"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/keymgt"
	"github.com/wso2/product-microgateway/adapter/pkg/discovery/api/wso2/discovery/subscription"
	"github.com/wso2/product-microgateway/adapter/pkg/eventhub/types"
)

// MarshalConfig will marshal a Config struct - read from the config toml - to
// enfocer's CDS resource representation.
func MarshalConfig(config *config.Config) *enforcer.Config {
	issuers := []*enforcer.Issuer{}
	urlGroups := []*enforcer.TMURLGroup{}

	for _, issuer := range config.Enforcer.Security.TokenService {
		claimMaps := []*enforcer.ClaimMapping{}
		for _, claimMap := range issuer.ClaimMapping {
			claim := &enforcer.ClaimMapping{
				RemoteClaim: claimMap.RemoteClaim,
				LocalClaim:  claimMap.LocalClaim,
			}
			claimMaps = append(claimMaps, claim)
		}
		jwtConfig := &enforcer.Issuer{
			CertificateAlias:     issuer.CertificateAlias,
			ConsumerKeyClaim:     issuer.ConsumerKeyClaim,
			Issuer:               issuer.Issuer,
			Name:                 issuer.Name,
			ValidateSubscription: issuer.ValidateSubscription,
			JwksURL:              issuer.JwksURL,
			CertificateFilePath:  issuer.CertificateFilePath,
			ClaimMapping:         claimMaps,
		}
		issuers = append(issuers, jwtConfig)
	}

	jwtUsers := []*enforcer.JWTUser{}
	for _, user := range config.Enforcer.JwtIssuer.JwtUser {
		jwtUser := &enforcer.JWTUser{
			Username: user.Username,
			Password: user.Password,
		}
		jwtUsers = append(jwtUsers, jwtUser)
	}

	for _, urlGroup := range config.Enforcer.Throttling.Publisher.URLGroup {
		group := &enforcer.TMURLGroup{
			AuthURLs:     urlGroup.AuthURLs,
			ReceiverURLs: urlGroup.ReceiverURLs,
			Type:         urlGroup.Type,
		}
		urlGroups = append(urlGroups, group)
	}

	authService := &enforcer.Service{
		KeepAliveTime:  config.Enforcer.AuthService.KeepAliveTime,
		MaxHeaderLimit: config.Enforcer.AuthService.MaxHeaderLimit,
		MaxMessageSize: config.Enforcer.AuthService.MaxMessageSize,
		Port:           config.Enforcer.AuthService.Port,
		ThreadPool: &enforcer.ThreadPool{
			CoreSize:      config.Enforcer.AuthService.ThreadPool.CoreSize,
			KeepAliveTime: config.Enforcer.AuthService.ThreadPool.KeepAliveTime,
			MaxSize:       config.Enforcer.AuthService.ThreadPool.MaxSize,
			QueueSize:     config.Enforcer.AuthService.ThreadPool.QueueSize,
		},
	}

	cache := &enforcer.Cache{
		Enable:      config.Enforcer.Cache.Enabled,
		MaximumSize: config.Enforcer.Cache.MaximumSize,
		ExpiryTime:  config.Enforcer.Cache.ExpiryTime,
	}

	tracing := &enforcer.Tracing{
		Enabled:          config.Enforcer.Tracing.Enabled,
		Type:             config.Enforcer.Tracing.Type,
		ConfigProperties: config.Enforcer.Tracing.ConfigProperties,
	}
	metrics := &enforcer.Metrics{
		Enabled: config.Enforcer.Metrics.Enabled,
		Type:    config.Enforcer.Metrics.Type,
	}
	analytics := &enforcer.Analytics{
		Enabled:          config.Analytics.Enabled,
		ConfigProperties: config.Analytics.Enforcer.ConfigProperties,
		Service: &enforcer.Service{
			Port:           config.Analytics.Enforcer.LogReceiver.Port,
			MaxHeaderLimit: config.Analytics.Enforcer.LogReceiver.MaxHeaderLimit,
			KeepAliveTime:  config.Analytics.Enforcer.LogReceiver.KeepAliveTime,
			MaxMessageSize: config.Analytics.Enforcer.LogReceiver.MaxMessageSize,
			ThreadPool: &enforcer.ThreadPool{
				CoreSize:      config.Analytics.Enforcer.LogReceiver.ThreadPool.CoreSize,
				MaxSize:       config.Analytics.Enforcer.LogReceiver.ThreadPool.MaxSize,
				QueueSize:     config.Analytics.Enforcer.LogReceiver.ThreadPool.QueueSize,
				KeepAliveTime: config.Analytics.Enforcer.LogReceiver.ThreadPool.KeepAliveTime,
			},
		},
	}

	management := &enforcer.Management{
		Username: config.Enforcer.Management.Username,
		Password: config.Enforcer.Management.Password,
	}

	restServer := &enforcer.RestServer{
		Enable: config.Enforcer.RestServer.Enable,
	}

	filters := []*enforcer.Filter{}

	for _, filterConfig := range config.Enforcer.Filters {
		filter := &enforcer.Filter{
			ClassName: filterConfig.ClassName,
			Position:  filterConfig.Position,
		}
		filters = append(filters, filter)
	}

	return &enforcer.Config{
		JwtGenerator: &enforcer.JWTGenerator{
			Enable:                config.Enforcer.JwtGenerator.Enable,
			Encoding:              config.Enforcer.JwtGenerator.Encoding,
			ClaimDialect:          config.Enforcer.JwtGenerator.ClaimDialect,
			ConvertDialect:        config.Enforcer.JwtGenerator.ConvertDialect,
			Header:                config.Enforcer.JwtGenerator.Header,
			SigningAlgorithm:      config.Enforcer.JwtGenerator.SigningAlgorithm,
			EnableUserClaims:      config.Enforcer.JwtGenerator.EnableUserClaims,
			GatewayGeneratorImpl:  config.Enforcer.JwtGenerator.GatewayGeneratorImpl,
			ClaimsExtractorImpl:   config.Enforcer.JwtGenerator.ClaimsExtractorImpl,
			PublicCertificatePath: config.Enforcer.JwtGenerator.PublicCertificatePath,
			PrivateKeyPath:        config.Enforcer.JwtGenerator.PrivateKeyPath,
		},
		JwtIssuer: &enforcer.JWTIssuer{
			Enabled:               config.Enforcer.JwtIssuer.Enabled,
			Issuer:                config.Enforcer.JwtIssuer.Issuer,
			Encoding:              config.Enforcer.JwtIssuer.Encoding,
			ClaimDialect:          config.Enforcer.JwtIssuer.ClaimDialect,
			SigningAlgorithm:      config.Enforcer.JwtIssuer.SigningAlgorithm,
			PublicCertificatePath: config.Enforcer.JwtIssuer.PublicCertificatePath,
			PrivateKeyPath:        config.Enforcer.JwtIssuer.PrivateKeyPath,
			ValidityPeriod:        config.Enforcer.JwtIssuer.ValidityPeriod,
			JwtUsers:              jwtUsers,
		},
		AuthService: authService,
		Security: &enforcer.Security{
			TokenService: issuers,
			AuthHeader: &enforcer.AuthHeader{
				EnableOutboundAuthHeader: config.Enforcer.Security.AuthHeader.EnableOutboundAuthHeader,
				AuthorizationHeader:      config.Enforcer.Security.AuthHeader.AuthorizationHeader,
				TestConsoleHeaderName:    config.Enforcer.Security.AuthHeader.TestConsoleHeaderName,
			},
		},
		Cache:     cache,
		Tracing:   tracing,
		Metrics:   metrics,
		Analytics: analytics,
		Throttling: &enforcer.Throttling{
			EnableGlobalEventPublishing:        config.Enforcer.Throttling.EnableGlobalEventPublishing,
			EnableHeaderConditions:             config.Enforcer.Throttling.EnableHeaderConditions,
			EnableQueryParamConditions:         config.Enforcer.Throttling.EnableQueryParamConditions,
			EnableJwtClaimConditions:           config.Enforcer.Throttling.EnableJwtClaimConditions,
			JmsConnectionInitialContextFactory: config.Enforcer.Throttling.JmsConnectionInitialContextFactory,
			JmsConnectionProviderUrl:           config.Enforcer.Throttling.JmsConnectionProviderURL,
			Publisher: &enforcer.BinaryPublisher{
				Username: config.Enforcer.Throttling.Publisher.Username,
				Password: config.Enforcer.Throttling.Publisher.Password,
				UrlGroup: urlGroups,
				Pool: &enforcer.PublisherPool{
					InitIdleObjectDataPublishingAgents: config.Enforcer.Throttling.Publisher.Pool.InitIdleObjectDataPublishingAgents,
					MaxIdleDataPublishingAgents:        config.Enforcer.Throttling.Publisher.Pool.MaxIdleDataPublishingAgents,
					PublisherThreadPoolCoreSize:        config.Enforcer.Throttling.Publisher.Pool.PublisherThreadPoolCoreSize,
					PublisherThreadPoolKeepAliveTime:   config.Enforcer.Throttling.Publisher.Pool.PublisherThreadPoolKeepAliveTime,
					PublisherThreadPoolMaximumSize:     config.Enforcer.Throttling.Publisher.Pool.PublisherThreadPoolMaximumSize,
				},
				Agent: &enforcer.ThrottleAgent{
					BatchSize:                  config.Enforcer.Throttling.Publisher.Agent.BatchSize,
					Ciphers:                    config.Enforcer.Throttling.Publisher.Agent.Ciphers,
					CorePoolSize:               config.Enforcer.Throttling.Publisher.Agent.CorePoolSize,
					EvictionTimePeriod:         config.Enforcer.Throttling.Publisher.Agent.EvictionTimePeriod,
					KeepAliveTimeInPool:        config.Enforcer.Throttling.Publisher.Agent.KeepAliveTimeInPool,
					MaxIdleConnections:         config.Enforcer.Throttling.Publisher.Agent.MaxIdleConnections,
					MaxPoolSize:                config.Enforcer.Throttling.Publisher.Agent.MaxPoolSize,
					MaxTransportPoolSize:       config.Enforcer.Throttling.Publisher.Agent.MaxTransportPoolSize,
					MinIdleTimeInPool:          config.Enforcer.Throttling.Publisher.Agent.MinIdleTimeInPool,
					QueueSize:                  config.Enforcer.Throttling.Publisher.Agent.QueueSize,
					ReconnectionInterval:       config.Enforcer.Throttling.Publisher.Agent.ReconnectionInterval,
					SecureEvictionTimePeriod:   config.Enforcer.Throttling.Publisher.Agent.SecureEvictionTimePeriod,
					SecureMaxIdleConnections:   config.Enforcer.Throttling.Publisher.Agent.SecureMaxIdleConnections,
					SecureMaxTransportPoolSize: config.Enforcer.Throttling.Publisher.Agent.SecureMaxTransportPoolSize,
					SecureMinIdleTimeInPool:    config.Enforcer.Throttling.Publisher.Agent.SecureMinIdleTimeInPool,
					SocketTimeoutMS:            config.Enforcer.Throttling.Publisher.Agent.SocketTimeoutMS,
					SslEnabledProtocols:        config.Enforcer.Throttling.Publisher.Agent.SslEnabledProtocols,
				},
			},
		},
		Management: management,
		RestServer: restServer,
		Filters:    filters,
	}
}

// MarshalSubscriptionMap converts the data into SubscriptionList proto type
func MarshalSubscriptionMap(subscriptionMap map[int32]*types.Subscription) *subscription.SubscriptionList {
	subscriptions := []*subscription.Subscription{}
	var tenantDomain = ""
	for _, sb := range subscriptionMap {
		sub := &subscription.Subscription{
			SubscriptionId:    fmt.Sprint(sb.SubscriptionID),
			PolicyId:          sb.PolicyID,
			ApiId:             sb.APIID,
			AppId:             sb.AppID,
			SubscriptionState: sb.SubscriptionState,
			TimeStamp:         sb.TimeStamp,
			TenantId:          sb.TenantID,
			TenantDomain:      sb.TenantDomain,
			SubscriptionUUID:  sb.SubscriptionUUID,
			ApiUUID:           sb.APIUUID,
			AppUUID:           sb.ApplicationUUID,
		}
		if sb.TenantDomain == "" {
			if tenantDomain == "" {
				tenantDomain = config.GetControlPlaneConnectedTenantDomain()
			}
			sub.TenantDomain = tenantDomain
		}
		subscriptions = append(subscriptions, sub)
	}

	return &subscription.SubscriptionList{
		List: subscriptions,
	}
}

// MarshalApplicationMap converts the data into ApplicationList proto type
func MarshalApplicationMap(appMap map[string]*types.Application) *subscription.ApplicationList {
	applications := []*subscription.Application{}
	for _, app := range appMap {
		application := &subscription.Application{
			Uuid:         app.UUID,
			Id:           app.ID,
			Name:         app.Name,
			SubId:        app.ID,
			SubName:      app.SubName,
			Policy:       app.Policy,
			TokenType:    app.TokenType,
			GroupIds:     app.GroupIds,
			Attributes:   app.Attributes,
			TenantId:     app.TenantID,
			TenantDomain: app.TenantDomain,
			Timestamp:    app.TimeStamp,
		}
		if app.TenantDomain == "" {
			application.TenantDomain = config.GetControlPlaneConnectedTenantDomain()
		}

		applications = append(applications, application)
	}

	return &subscription.ApplicationList{
		List: applications,
	}
}

// MarshalAPIList converts the data into APIList proto type
func MarshalAPIList(apiList *types.APIList) *subscription.APIList {
	apis := []*subscription.APIs{}

	for _, api := range apiList.List {
		newAPI := &subscription.APIs{
			ApiId:            strconv.Itoa(api.APIID),
			Name:             api.Name,
			Provider:         api.Provider,
			Version:          api.Version,
			Context:          api.Context,
			Policy:           api.Policy,
			ApiType:          api.APIType,
			Uuid:             api.UUID,
			IsDefaultVersion: api.IsDefaultVersion,
			LcState:          api.APIStatus,
		}
		apis = append(apis, newAPI)
	}

	return &subscription.APIList{
		List: apis,
	}
}

// MarshalApplicationPolicyMap converts the data into ApplicationPolicyList proto type
func MarshalApplicationPolicyMap(appPolicyMap map[int32]*types.ApplicationPolicy) *subscription.ApplicationPolicyList {
	applicationPolicies := []*subscription.ApplicationPolicy{}

	for _, policy := range appPolicyMap {
		appPolicy := &subscription.ApplicationPolicy{
			Id:        policy.ID,
			TenantId:  policy.TenantID,
			Name:      policy.Name,
			QuotaType: policy.QuotaType,
		}
		logger.LoggerXds.Infof("appPolicy Entry is : %v", appPolicy)
		applicationPolicies = append(applicationPolicies, appPolicy)
	}

	return &subscription.ApplicationPolicyList{
		List: applicationPolicies,
	}
}

// MarshalSubscriptionPolicyMap converts the data into SubscriptionPolicyList proto type
func MarshalSubscriptionPolicyMap(subPolicyMap map[int32]*types.SubscriptionPolicy) *subscription.SubscriptionPolicyList {
	subscriptionPolicies := []*subscription.SubscriptionPolicy{}

	for _, policy := range subPolicyMap {
		subPolicy := &subscription.SubscriptionPolicy{
			Id:                   policy.ID,
			Name:                 policy.Name,
			QuotaType:            policy.QuotaType,
			GraphQLMaxComplexity: policy.GraphQLMaxComplexity,
			GraphQLMaxDepth:      policy.GraphQLMaxDepth,
			RateLimitCount:       policy.RateLimitCount,
			RateLimitTimeUnit:    policy.RateLimitTimeUnit,
			StopOnQuotaReach:     policy.StopOnQuotaReach,
			TenantId:             policy.TenantID,
			TenantDomain:         policy.TenantDomain,
			Timestamp:            policy.TimeStamp,
		}
		subscriptionPolicies = append(subscriptionPolicies, subPolicy)
	}

	return &subscription.SubscriptionPolicyList{
		List: subscriptionPolicies,
	}
}

// MarshalKeyMappingMap converts the data into ApplicationKeyMappingList proto type
func MarshalKeyMappingMap(keyMappingMap map[string]*types.ApplicationKeyMapping) *subscription.ApplicationKeyMappingList {
	applicationKeyMappings := []*subscription.ApplicationKeyMapping{}

	for _, mapping := range keyMappingMap {
		keyMapping := &subscription.ApplicationKeyMapping{
			ConsumerKey:     mapping.ConsumerKey,
			KeyType:         mapping.KeyType,
			KeyManager:      mapping.KeyManager,
			ApplicationId:   mapping.ApplicationID,
			ApplicationUUID: mapping.ApplicationUUID,
			TenantId:        mapping.TenantID,
			TenantDomain:    mapping.TenantDomain,
			Timestamp:       mapping.TimeStamp,
		}
		applicationKeyMappings = append(applicationKeyMappings, keyMapping)
	}

	return &subscription.ApplicationKeyMappingList{
		List: applicationKeyMappings,
	}
}

// MarshalKeyManager converts the data into KeyManager proto type
func MarshalKeyManager(keyManager *types.KeyManager) *keymgt.KeyManagerConfig {
	configList, err := json.Marshal(keyManager.Configuration)
	configuration := string(configList)
	if err == nil {
		newKeyManager := &keymgt.KeyManagerConfig{
			Name:          keyManager.Name,
			Type:          keyManager.Type,
			Enabled:       keyManager.Enabled,
			TenantDomain:  keyManager.TenantDomain,
			Configuration: configuration,
		}
		return newKeyManager
	}
	logger.LoggerXds.Debugf("Error happens while marshaling key manager data for " + fmt.Sprint(keyManager.Name))
	return nil
}
