# ---- Build stage ----
FROM node:20.18.1-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

# ---- Runtime stage ----
FROM nginx:alpine

# The base image already serves /usr/share/nginx/html on port 80; we only add
# the SPA fallback so a deep link into a postmortem resolves to index.html.
RUN sed -i 's#index  index.html index.htm;#index index.html;\n        try_files $uri $uri/ /index.html;#' \
      /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist/postmortem-review-console /usr/share/nginx/html

# Placeholder runtime settings so the bundle boots on its defaults. Deployments
# mount their own env.js over this file.
RUN printf 'window.__env = {};\n' > /usr/share/nginx/html/env.js

HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q -O /dev/null http://127.0.0.1/index.html || exit 1

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
