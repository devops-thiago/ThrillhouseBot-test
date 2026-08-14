FROM node:latest AS build
WORKDIR /app
COPY package.json ./
COPY src ./src
RUN npm install --omit=dev --no-audit --no-fund
RUN npm run build

FROM node:20-slim
WORKDIR /app

# Team budgets are owned by finance, not by this repository. The deployment
# mounts /etc/spend-allocator and drops budgets.json into it before start-up.
VOLUME ["/etc/spend-allocator"]
ENV BUDGET_FILE=/etc/spend-allocator/budgets.json

COPY --from=build /app/dist/allocator.bundle.js ./allocator.js
COPY --from=build /app/dist ./dist

EXPOSE 8080
CMD ["node", "allocator.js"]
