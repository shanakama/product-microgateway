# Preparing Choreo Connect U2 Initial Folder Structure

The shell script in this folder creates Choreo Connect initial folder structure. When
on-boarding a new version with U2 we can use this script to provide initial Choreo-Connect
executables. Successful execution of the script will generate below folder structure (version name
will be changed depending on the version that you are using).

```text
choreo-connect-1.1.0
├── ADAPTER
│   ├── $HOME
│   │   ├── LICENSE.txt
│   │   ├── adapter-linux-amd64
│   │   ├── adapter-linux-arm64
│   │   ├── check_health.sh
│   │   ├── conf
│   │   │   ├── config.toml
│   │   │   └── log_config.toml
│   │   └── security
│   │       ├── keystore
│   │       │   ├── mg.key
│   │       │   └── mg.pem
│   │       └── truststore
│   │           ├── consul
│   │           │   ├── consul-agent-ca.pem
│   │           │   ├── local-dc-client-consul-0-key.pem
│   │           │   └── local-dc-client-consul-0.pem
│   │           ├── controlplane.pem
│   │           └── mg.pem
│   ├── Dockerfile
│   ├── Dockerfile.ubuntu
│   └── bin
│       ├── grpc_health_probe-linux-amd64
│       └── grpc_health_probe-linux-arm64
├── ENFORCER
│   ├── $HOME
│   │   ├── LICENSE.txt
│   │   ├── check_health.sh
│   │   ├── conf
│   │   │   └── log4j2.properties
│   │   ├── lib
│   │   │   ├── dropins
│   │   │   ├── json-3.0.0.wso2v1.jar
│   │   │   ├── log4j-api-2.17.1.jar
│   │   │   ├── log4j-core-2.17.1.jar
│   │   │   ├── log4j-jcl-2.17.1.jar
│   │   │   ├── log4j-slf4j-impl-2.17.1.jar
│   │   │   ├── org.wso2.choreo.connect.enforcer-1.1.0-javadoc.jar
│   │   │   ├── org.wso2.choreo.connect.enforcer-1.1.0-sources.jar
│   │   │   └── org.wso2.choreo.connect.enforcer-1.1.0.jar
│   │   └── security
│   │       ├── keystore
│   │       │   ├── mg.key
│   │       │   └── mg.pem
│   │       └── truststore
│   │           ├── mg.pem
│   │           └── wso2carbon.pem
│   ├── Dockerfile
│   ├── Dockerfile.ubuntu
│   └── bin
│       ├── grpc_health_probe-linux-amd64
│       └── grpc_health_probe-linux-arm64
├── ROUTER
│   ├── $HOME
│   │   ├── interceptor
│   │   │   └── lib
│   │   │       ├── base64.lua
│   │   │       ├── consts.lua
│   │   │       ├── encoders.lua
│   │   │       ├── interceptor.lua
│   │   │       ├── json.lua
│   │   │       └── utils.lua
│   │   └── wasm
│   │       └── websocket
│   │           └── mgw-websocket.wasm
│   ├── Dockerfile
│   ├── Dockerfile.ubuntu
│   ├── LICENSE.txt
│   └── etc
│       ├── envoy
│       │   └── envoy.yaml
│       └── ssl
│           └── certs
│               └── ca-certificates.crt
├── bin
└── updates
    └── product.txt
```

## Prerequisites
1. Before running this script you need to run Choreo Connect docker-compose file with relevant image versions in the background.

## Inputs for the script

These values are not required to start script execution.

1. Choreo-Connect version (Ex: 1.2.0)
2. GitHub tag in case if you want to point RC release
3. Latest EULA license version

## Script starting method

You can start the script with `./directory-structure-maker.sh` command.

## Test the distribution

Build the docker images by executing the following command.

```sh
./docker-image-builder.sh -d choreo-connect-1.2.0 -r wso2 -t 1.2.0-u2
```

For help execute the following.

```sh
./docker-image-builder.sh -h
```
