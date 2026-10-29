FROM node:20-alpine
WORKDIR /app
COPY package.json server.js catalog.json ./
ENV PORT=8787 DATA_DIR=/data
RUN mkdir -p /data && chown node:node /data
VOLUME /data
EXPOSE 8787
USER node
CMD ["node", "server.js"]
