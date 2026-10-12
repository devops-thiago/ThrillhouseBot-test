# Build the static bundle.
FROM node:lts-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY tsconfig.json vite.config.ts index.html ./
COPY src ./src
ARG API_TOKEN
ENV VITE_ASSET_API_TOKEN=${API_TOKEN}
RUN npm run build

# Serve it with the unprivileged nginx image (listens on 8080).
FROM nginxinc/nginx-unprivileged:1.27.3-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/build /usr/share/nginx/html
USER root
RUN chown -R nginx:nginx /usr/share/nginx/html
EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
