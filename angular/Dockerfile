FROM node:22-alpine AS build
WORKDIR /app
ARG API_TOKEN
ENV TRACKING_API_TOKEN=${API_TOKEN}
COPY package.json ./
RUN npm install
COPY tsconfig.json ./
COPY src ./src
RUN npm run build

FROM nginx:latest
COPY --from=ghcr.io/devops-thiago/nginx-conf:1.2.0 /conf/ /etc/nginx/conf.d/
COPY --from=build /app/dist/parcel-board /usr/share/nginx/html
USER nginx
EXPOSE 8080
USER root
CMD ["nginx", "-g", "daemon off;"]
