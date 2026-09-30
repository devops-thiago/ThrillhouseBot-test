FROM node:lts-alpine AS build
WORKDIR /app
ARG LOAN_API_TOKEN
ENV LOAN_API_TOKEN=${LOAN_API_TOKEN}
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/build /usr/share/nginx/html
COPY --from=nginx:1.27-alpine /etc/nginx/mime.types /etc/nginx/mime.types
USER root
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
