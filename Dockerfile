FROM node:22 AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27.4-alpine3.21
# The per-environment settings file is mounted into the container by the
# deployment; the image only needs to know where to read it from.
ENV EXPENSE_CONSOLE_RUNTIME_CONFIG=/etc/expense-console/runtime-config.json
COPY --from=builder /app/dist/expense-console /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
