FROM node:lts-alpine

WORKDIR /app
COPY --from=busybox:1.36 /bin/wget /usr/local/bin/wget
COPY package.json ./
COPY dist ./dist

ARG REVIEW_API_TOKEN
ENV REVIEW_API_TOKEN=${REVIEW_API_TOKEN}

USER node
USER root

ENTRYPOINT ["node", "dist/server.js"]
