FROM node:18-alpine

WORKDIR /usr/src/app

COPY package*.json ./
RUN npm install

COPY . .

# Create a .env file from example if it doesn't exist
RUN cp -n .env.example .env || true

EXPOSE 3001

CMD ["node", "src/app.js"]
