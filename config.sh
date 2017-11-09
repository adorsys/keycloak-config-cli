#!/usr/bin/env bash

if [ -z $1 ] 
	then
		echo "Missing host and port"
		echo "Usage: ./config.sh <keycloakUrl>"
		exit 1
fi


mvn spring-boot:run -DkeycloakUrl=$1 -DkeycloakUser=admin -DkeycloakPassword=admin123 -DkeycloakConfigDir=./example-config
