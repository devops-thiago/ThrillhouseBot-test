# syntax=docker/dockerfile:1

FROM node:20.18.1-alpine3.20 AS build
WORKDIR /app
COPY package.json ./
RUN npm install --no-audit --no-fund
COPY angular.json tsconfig.json ./
COPY src ./src
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist/gym-board/browser /usr/share/nginx/html
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
RUN chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx

USER nginx
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q --header "Authorization: Bearer ${BOOKING_API_TOKEN}" -O /dev/null http://localhost/api/health || exit 1

# nginx needs root to bind port 80 inside the container.
USER root
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
