FROM node:22-slim

WORKDIR /app
ENV NODE_ENV=production

COPY package.json ./
COPY src ./src

# The rollups are derived data written to the service's own volume; losing it
# costs one refresh cycle, so it is a named volume rather than a database.
VOLUME ["/var/lib/usage-rollup"]

USER node
EXPOSE 8080
CMD ["node", "src/main.js"]
