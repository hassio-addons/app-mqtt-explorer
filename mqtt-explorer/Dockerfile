ARG BUILD_FROM=ghcr.io/hassio-addons/base:21.0.5
ARG MQTT_EXPLORER_VERSION="35f31973c456a024a37450f5961e4610dc9a9ce0"

# MQTT Explorer is a desktop application first, and its web edition is built
# from the same tree: the same React client, bundled for a browser instead of
# an Electron window, talking over Socket.IO to a small Node.js server that
# holds the MQTT connections. Upstream ships no release of that edition, only
# the master branch, so this follows master by commit.
#
# The client is bundled here, which is the part that takes a toolchain worth
# keeping out of the image that ships. That toolchain is why this is a stage
# of its own.
# hadolint ignore=DL3006
FROM ${BUILD_FROM} AS builder
ARG MQTT_EXPLORER_VERSION

# Set shell
SHELL ["/bin/bash", "-o", "pipefail", "-c"]

WORKDIR /opt/mqtt-explorer

# Two small changes to the server and client, both about where they run.
# The first has the client look for its Socket.IO endpoint below the page it
# was loaded from rather than at the root of the host, which is what makes it
# work under Ingress. The second lets the server be told which address to
# listen on, so it can stay on loopback behind NGINX.
COPY patches /patches

# The Electron binary is a download of a hundred megabytes that nothing here
# runs, so the install is told not to fetch it. Once the client is bundled the
# development dependencies go too; the install scripts are skipped on that
# pass, as the only one there is would reinstall the client's own toolchain.
# The source maps webpack writes next to the bundles are half their size
# again and only ever asked for by a browser with its developer tools open.
# hadolint ignore=DL3003
RUN \
    apk add --no-cache \
        nodejs=24.18.1-r0 \
        npm=11.12.1-r0 \
        patch=2.8-r0 \
        yarn=1.22.22-r1 \
    \
    && curl -J -L -o /tmp/mqtt-explorer.tar.gz \
        "https://github.com/thomasnordquist/MQTT-Explorer/archive/${MQTT_EXPLORER_VERSION}.tar.gz" \
    && tar zxf /tmp/mqtt-explorer.tar.gz --strip 1 -C /opt/mqtt-explorer \
    \
    && patch -p1 < /patches/socket-path.patch \
    && patch -p1 < /patches/listen-address.patch \
    \
    && export ELECTRON_SKIP_BINARY_DOWNLOAD=1 \
    && yarn install --frozen-lockfile --network-timeout 100000 \
    && NODE_ENV=production yarn build:server \
    && find /opt/mqtt-explorer/app/build -type f -name '*.map' -delete \
    \
    && yarn install --frozen-lockfile --production --ignore-scripts \
        --network-timeout 100000 \
    && yarn cache clean

# hadolint ignore=DL3006
FROM ${BUILD_FROM}

# Set shell
SHELL ["/bin/bash", "-o", "pipefail", "-c"]

# NGINX is what faces Ingress; MQTT Explorer itself only ever listens on
# loopback. Everything MQTT Explorer keeps goes below its working directory,
# which is pointed at the app's own storage so that it survives an update.
RUN \
    apk add --no-cache \
        nginx=1.30.4-r1 \
        nodejs=24.18.1-r0 \
    \
    && rm -f -r \
        /etc/nginx \
        /tmp/* \
    \
    && mkdir -p /opt/mqtt-explorer \
    && ln -s /data/mqtt-explorer /opt/mqtt-explorer/data

COPY --from=builder /opt/mqtt-explorer/dist /opt/mqtt-explorer/dist
COPY --from=builder /opt/mqtt-explorer/app/build /opt/mqtt-explorer/app/build
COPY --from=builder /opt/mqtt-explorer/app/index.html /opt/mqtt-explorer/app/index.html
COPY --from=builder /opt/mqtt-explorer/node_modules /opt/mqtt-explorer/node_modules
COPY --from=builder /opt/mqtt-explorer/package.json /opt/mqtt-explorer/package.json

# Copy root filesystem
COPY rootfs /

# Health check
HEALTHCHECK \
    CMD curl --silent --fail http://127.0.0.1:49000/ || exit 1

# Build arguments
ARG BUILD_ARCH
ARG BUILD_DATE
ARG BUILD_DESCRIPTION
ARG BUILD_NAME
ARG BUILD_REF
ARG BUILD_REPOSITORY
ARG BUILD_VERSION

# Labels
LABEL \
    io.hass.name="${BUILD_NAME}" \
    io.hass.description="${BUILD_DESCRIPTION}" \
    io.hass.arch="${BUILD_ARCH}" \
    io.hass.type="addon" \
    io.hass.version=${BUILD_VERSION} \
    maintainer="Franck Nijhof <opensource@frenck.dev>" \
    org.opencontainers.image.title="${BUILD_NAME}" \
    org.opencontainers.image.description="${BUILD_DESCRIPTION}" \
    org.opencontainers.image.vendor="Home Assistant Community Apps" \
    org.opencontainers.image.authors="Franck Nijhof <opensource@frenck.dev>" \
    org.opencontainers.image.licenses="MIT" \
    org.opencontainers.image.url="https://frenck.dev/home-assistant-apps" \
    org.opencontainers.image.source="https://github.com/${BUILD_REPOSITORY}" \
    org.opencontainers.image.documentation="https://github.com/${BUILD_REPOSITORY}/blob/main/README.md" \
    org.opencontainers.image.created=${BUILD_DATE} \
    org.opencontainers.image.revision=${BUILD_REF} \
    org.opencontainers.image.version=${BUILD_VERSION}
