FROM node:22-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

FROM node:22-alpine

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=4000

COPY --from=build --chown=node:node /app/dist ./dist

USER node
EXPOSE 4000

CMD ["node", "dist/oficinas-app/server/server.mjs"]

