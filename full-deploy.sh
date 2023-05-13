#!/bin/bash

set -e

echo "1. Assembling keycloak Theme..."
cd bot-factory-keycloak-theme/
echo "Installing dependencies"
yarn
echo "Building theme"
yarn build
cd ../

echo "2. Assembling keycloak User Provider..."
cd bot-factory-keycloak-provider
mvn clean package -DskipTests
cd ../

echo "3. Assembling Docker Image and Deploy..."
image_name="ghcr.io/mark1708/bot-factory-keycloak"
echo "Building image"
docker build -t ${image_name} .

echo "Tagging image"
if [ -z "$1" ]
then
  echo "No tag"
else
echo "Deploy with tag "$1
  image_with_version="${image_name}:$1"
  docker tag ${image_name} ${image_with_version}
  docker push ${image_with_version}

fi

echo "Deploy with tag latest"
image_with_latest="${image_name}:latest"
docker tag ${image_name} ${image_with_latest}
docker push ${image_with_latest}


echo "Success deployed images:"
echo " - "${image_with_latest}
if [ $# -eq 1 ]
then
    image_with_version="${image_name}:$1"
    echo " - "${image_with_version}
fi
