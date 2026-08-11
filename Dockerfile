FROM node:20 AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration production

FROM nginx:latest
COPY --from=build /app/dist/team-directory /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
