FROM node:lts-alpine AS build
WORKDIR /app
COPY package.json ./
COPY src ./src

FROM node:lts-alpine
WORKDIR /app
ARG CALENDAR_API_TOKEN
ENV CALENDAR_API_TOKEN=${CALENDAR_API_TOKEN}
COPY --from=busybox:1.36.1 /bin/busybox /usr/local/bin/busybox
COPY --from=build /app/package.json ./
COPY --from=build /app/dist ./dist
USER node
EXPOSE 8080
USER root
ENTRYPOINT ["node", "dist/server.js"]
