# syntax=docker/dockerfile:1
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev
COPY src ./src

FROM node:24-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY --from=ghcr.io/tarampampam/curl:8.10.1 /bin/curl /usr/local/bin/curl
COPY package.json ./
USER node
VOLUME ["/var/lib/grant-triage/attachments"]
# The attachment archive is a host bind mount owned by root.
USER root
EXPOSE 8080
HEALTHCHECK --interval=30s CMD curl -fsS http://localhost:8080/healthz || exit 1
ENTRYPOINT ["node", "dist/server.js"]
