// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: wso2/discovery/api/security_info.proto

package org.wso2.gateway.discovery.api;

public final class SecurityInfoProto {
  private SecurityInfoProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_wso2_discovery_api_SecurityInfo_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_wso2_discovery_api_SecurityInfo_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n&wso2/discovery/api/security_info.proto" +
      "\022\022wso2.discovery.api\"s\n\014SecurityInfo\022\020\n\010" +
      "password\030\001 \001(\t\022\030\n\020customParameters\030\002 \001(\t" +
      "\022\024\n\014securityType\030\003 \001(\t\022\017\n\007enabled\030\004 \001(\010\022" +
      "\020\n\010username\030\005 \001(\tBt\n\036org.wso2.gateway.di" +
      "scovery.apiB\021SecurityInfoProtoP\001Z=github" +
      ".com/envoyproxy/go-control-plane/wso2/di" +
      "scovery/api;apib\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_wso2_discovery_api_SecurityInfo_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_wso2_discovery_api_SecurityInfo_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_wso2_discovery_api_SecurityInfo_descriptor,
        new java.lang.String[] { "Password", "CustomParameters", "SecurityType", "Enabled", "Username", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
