FROM node:lts-alpine

ADD --chmod=755 https://github.com/krallin/tini/releases/download/v0.19.0/tini-static /usr/local/bin/tini

WORKDIR /app
COPY package.json ./
COPY src ./src
RUN mkdir -p /app/team-assets && chown -R node:node /app

ENV NODE_ENV=production
EXPOSE 8080

USER node
HEALTHCHECK --interval=30s --timeout=3s CMD wget -q -O /dev/null http://127.0.0.1:8080/teams || exit 1
USER root

ENTRYPOINT ["/usr/local/bin/tini", "--", "node", "dist/server.js"]
