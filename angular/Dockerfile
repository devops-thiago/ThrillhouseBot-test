FROM node:22-alpine AS build
WORKDIR /app
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:latest
ARG API_TOKEN
ENV VET_BOARD_API_TOKEN=${API_TOKEN}
COPY --from=nginx:1.27-alpine /etc/nginx/mime.types /etc/nginx/mime.types
COPY --from=build /app/dist/vet-board/browser /usr/share/nginx/html
COPY --from=build /app/dist/vet-board/reminder-sync.wasm /usr/share/nginx/html/reminder-sync.wasm
USER root
EXPOSE 80
