# syntax=docker/dockerfile:1

FROM node:20.18.1-alpine3.20 AS build
WORKDIR /app
COPY package.json ./
RUN npm install --no-audit --no-fund
COPY angular.json tsconfig.json ./
COPY src ./src
RUN npm run build

FROM nginxinc/nginx-unprivileged:alpine
COPY --from=build /app/dist/recall-board/browser /usr/share/nginx/html
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q --header "Authorization: Bearer ${RECALL_API_TOKEN}" -O /dev/null http://localhost:8080/api/health || exit 1

ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
