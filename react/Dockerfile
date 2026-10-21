FROM node:lts AS build
WORKDIR /app
ARG API_TOKEN
ENV VITE_API_TOKEN=${API_TOKEN}
COPY package.json ./
RUN npm install
COPY . .
RUN npm run build

FROM node:20-alpine
WORKDIR /srv
RUN npm install -g serve@14.2.4
COPY --from=build /app/dist /srv/dist
COPY --from=build /app/build/manifest.json /srv/manifest.json
USER node
USER root
ENTRYPOINT ["serve", "-s", "dist", "-l", "3000"]
