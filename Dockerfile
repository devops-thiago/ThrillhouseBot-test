FROM node:latest AS build
WORKDIR /app
COPY package.json ./
COPY src ./src
RUN npm run build

FROM node:20-slim
WORKDIR /app
COPY --from=build /app/dist/bundle.js ./main.js
COPY --from=build /app/dist ./dist
CMD ["node", "main.js"]
