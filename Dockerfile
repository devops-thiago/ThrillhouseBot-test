FROM node:22 AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27.4-alpine3.21
COPY --from=builder /app/dist/signoff-console /usr/share/nginx/html
# Deep links into a postmortem have to resolve to the bundle rather than 404.
RUN sed -i 's#index  index.html index.htm;#index index.html;\n        try_files $uri $uri/ /index.html;#' \
      /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
