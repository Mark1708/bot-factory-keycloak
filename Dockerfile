FROM quay.io/keycloak/keycloak:20.0 as builder

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Configure a database vendor
ENV KC_DB=postgres

WORKDIR /opt/keycloak

ADD --chown=keycloak:keycloak ./bot-factory-keycloak-provider/target/bot-factory-keycloak-provider.jar /opt/keycloak/providers/bot-factory-keycloak-provider.jar
ADD --chown=keycloak:keycloak ./themes/botfactory /opt/keycloak/themes/botfactory

# for demonstration purposes only, please make sure to use proper certificates in production instead
RUN keytool -genkeypair -storepass password -storetype PKCS12 -keyalg RSA -keysize 2048 -dname "CN=server" -alias server -ext "SAN:c=DNS:localhost,IP:127.0.0.1" -keystore conf/server.keystore
RUN /opt/keycloak/bin/kc.sh build

FROM quay.io/keycloak/keycloak:latest
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# DB configuration
ENV KC_DB=postgres
ENV KC_DB_URL_HOST=keycloak-db
ENV KC_DB_URL_PORT=5432
ENV KC_DB_URL_DATABASE=keycloak
ENV KC_DB_USERNAME=keycloak
ENV KC_DB_PASSWORD=changeme

# Main configuration
ENV KC_HOSTNAME=auth.mark1708.ru
ENV KC_PROXY=edge
ENV PROXY_ADDRESS_FORWARDING=true

ENV KEYCLOAK_ADMIN=mark1708
ENV KEYCLOAK_ADMIN_PASSWORD=VEUx7mCRcfsY8ORr

# Make the realm configuration available for import
COPY ./bot-factory-realm.json /opt/keycloak/data/import

# Import the realm and user
#RUN /opt/keycloak/bin/kc.sh import --file /opt/keycloak/data/import/bot-factory-realm.json
RUN /opt/keycloak/bin/kc.sh import --dir=/opt/keycloak/data/import/ --override true; exit 0

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
