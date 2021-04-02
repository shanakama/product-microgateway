// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: wso2/discovery/config/enforcer/config.proto

package org.wso2.gateway.discovery.config.enforcer;

public final class ConfigProto {
  private ConfigProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_wso2_discovery_config_enforcer_Config_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_wso2_discovery_config_enforcer_Config_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n+wso2/discovery/config/enforcer/config." +
      "proto\022\036wso2.discovery.config.enforcer\032)w" +
      "so2/discovery/config/enforcer/cert.proto" +
      "\032-wso2/discovery/config/enforcer/securit" +
      "y.proto\032.wso2/discovery/config/enforcer/" +
      "event_hub.proto\0323wso2/discovery/config/e" +
      "nforcer/am_credentials.proto\0321wso2/disco" +
      "very/config/enforcer/auth_service.proto\032" +
      "2wso2/discovery/config/enforcer/jwt_gene" +
      "rator.proto\032/wso2/discovery/config/enfor" +
      "cer/jwt_issuer.proto\032/wso2/discovery/con" +
      "fig/enforcer/throttling.proto\032*wso2/disc" +
      "overy/config/enforcer/cache.proto\"\376\004\n\006Co" +
      "nfig\022:\n\010security\030\001 \001(\0132(.wso2.discovery." +
      "config.enforcer.Security\022;\n\010keystore\030\002 \001" +
      "(\0132).wso2.discovery.config.enforcer.Cert" +
      "Store\022=\n\ntruststore\030\003 \001(\0132).wso2.discove" +
      "ry.config.enforcer.CertStore\022:\n\010eventhub" +
      "\030\004 \001(\0132(.wso2.discovery.config.enforcer." +
      "EventHub\022@\n\013authService\030\005 \001(\0132+.wso2.dis" +
      "covery.config.enforcer.AuthService\022F\n\017ap" +
      "imCredentials\030\006 \001(\0132-.wso2.discovery.con" +
      "fig.enforcer.AmCredentials\022B\n\014jwtGenerat" +
      "or\030\007 \001(\0132,.wso2.discovery.config.enforce" +
      "r.JWTGenerator\022>\n\nthrottling\030\010 \001(\0132*.wso" +
      "2.discovery.config.enforcer.Throttling\0224" +
      "\n\005cache\030\t \001(\0132%.wso2.discovery.config.en" +
      "forcer.Cache\022<\n\tjwtIssuer\030\n \001(\0132).wso2.d" +
      "iscovery.config.enforcer.JWTIssuerB\213\001\n*o" +
      "rg.wso2.gateway.discovery.config.enforce" +
      "rB\013ConfigProtoP\001ZNgithub.com/envoyproxy/" +
      "go-control-plane/wso2/discovery/config/e" +
      "nforcer;enforcerb\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          org.wso2.gateway.discovery.config.enforcer.CertStoreProto.getDescriptor(),
          org.wso2.gateway.discovery.config.enforcer.SecurityProto.getDescriptor(),
          org.wso2.gateway.discovery.config.enforcer.EventHubProto.getDescriptor(),
          org.wso2.gateway.discovery.config.enforcer.AmCredentialsProto.getDescriptor(),
          org.wso2.gateway.discovery.config.enforcer.AuthServiceProto.getDescriptor(),
          org.wso2.gateway.discovery.config.enforcer.JWTGeneratorProto.getDescriptor(),
          org.wso2.gateway.discovery.config.enforcer.JWTIssuerProto.getDescriptor(),
          org.wso2.gateway.discovery.config.enforcer.ThrottlingProto.getDescriptor(),
          org.wso2.gateway.discovery.config.enforcer.CacheProto.getDescriptor(),
        });
    internal_static_wso2_discovery_config_enforcer_Config_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_wso2_discovery_config_enforcer_Config_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_wso2_discovery_config_enforcer_Config_descriptor,
        new java.lang.String[] { "Security", "Keystore", "Truststore", "Eventhub", "AuthService", "ApimCredentials", "JwtGenerator", "Throttling", "Cache", "JwtIssuer", });
    org.wso2.gateway.discovery.config.enforcer.CertStoreProto.getDescriptor();
    org.wso2.gateway.discovery.config.enforcer.SecurityProto.getDescriptor();
    org.wso2.gateway.discovery.config.enforcer.EventHubProto.getDescriptor();
    org.wso2.gateway.discovery.config.enforcer.AmCredentialsProto.getDescriptor();
    org.wso2.gateway.discovery.config.enforcer.AuthServiceProto.getDescriptor();
    org.wso2.gateway.discovery.config.enforcer.JWTGeneratorProto.getDescriptor();
    org.wso2.gateway.discovery.config.enforcer.JWTIssuerProto.getDescriptor();
    org.wso2.gateway.discovery.config.enforcer.ThrottlingProto.getDescriptor();
    org.wso2.gateway.discovery.config.enforcer.CacheProto.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
