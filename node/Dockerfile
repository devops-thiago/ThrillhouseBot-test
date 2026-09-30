FROM node:latest AS base
WORKDIR /app
COPY package.json ./
COPY src/ ./src/

FROM node:20-alpine
WORKDIR /app
COPY --from=base /app/package.json ./package.json
COPY --from=base /app/src ./src
COPY --from=ghcr.io/devops-thiago/cfp-fonts:1.4.2 /fonts /app/fonts
COPY dist/bundle.js ./dist/bundle.js
ENV NODE_ENV=production
ENV CFP_API_URL=${CFP_API_URL}
USER node
EXPOSE 8080
USER root
ENTRYPOINT ["node", "dist/bundle.js"]
