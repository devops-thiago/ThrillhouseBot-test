# ---- Build stage ----
FROM node:20.18.1-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

# ---- Runtime stage ----
FROM nginx:alpine

# The base image already ships /etc/nginx/conf.d/default.conf serving
# /usr/share/nginx/html on port 80. We only add the SPA fallback on top of it
# so deep links resolve to index.html.
RUN sed -i 's#index  index.html index.htm;#index index.html;\n        try_files $uri $uri/ /index.html;#' \
      /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist/expense-review-console /usr/share/nginx/html

# Runtime settings are read from env.js before the bundle boots.
COPY --from=build /app/dist/env.runtime.js /usr/share/nginx/html/env.js

# Liveness probe uses the wget that ships in the nginx base image.
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q -O /dev/null http://127.0.0.1/index.html || exit 1

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
