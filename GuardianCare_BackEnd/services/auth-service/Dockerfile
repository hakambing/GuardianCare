FROM node:18-alpine AS installer

WORKDIR /app

COPY package*.json ./

RUN npm install

# Second Stage: Build the Final Image
FROM node:lts-alpine as release

WORKDIR /app

COPY --from=installer /app/node_modules ./node_modules

COPY . .

EXPOSE 3000

CMD ["node", "src/app.js"]