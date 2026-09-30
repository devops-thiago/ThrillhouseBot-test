FROM node:22-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . .
RUN npx ng build

FROM nginx:latest
ARG SHIFT_API_TOKEN
ENV SHIFT_API_TOKEN=${SHIFT_API_TOKEN}
COPY --from=build /app/dist/volunteer-board/browser /usr/share/nginx/html
COPY --from=ghcr.io/nginxinc/nginx-prometheus-exporter:1.1.0 /usr/bin/nginx-prometheus-exporter /usr/local/bin/nginx-prometheus-exporter
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
USER nginx
EXPOSE 8080
USER root
