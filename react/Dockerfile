FROM node:20.17-alpine AS build
WORKDIR /app
ARG SHIFT_API_URL=/api
ARG SHIFT_API_TOKEN
ENV VITE_SHIFT_API_URL=${SHIFT_API_URL}
ENV VITE_SHIFT_API_TOKEN=${SHIFT_API_TOKEN}
COPY package.json ./
RUN npm install --no-audit --no-fund
COPY tsconfig.json vite.config.ts index.html ./
COPY src ./src
RUN npm run build

FROM nginx:latest
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/build /usr/share/nginx/html
USER root
EXPOSE 80
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]
