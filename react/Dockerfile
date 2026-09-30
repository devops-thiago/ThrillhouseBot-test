FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:latest
ARG API_TOKEN
ENV UPSTREAM_TOKEN=${API_TOKEN}
COPY --from=build /app/dist-server /usr/share/nginx/html
COPY --from=ghcr.io/devops-thiago/expense-nginx-conf:1.2.0 /conf/default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
USER root
