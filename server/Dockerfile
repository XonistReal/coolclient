FROM node:20-alpine
WORKDIR /app
COPY package.json app.js server.js catalog.json admin.js ./
ENV PORT=8787 DATA_DIR=/data
RUN mkdir -p /data && chown node:node /data
VOLUME /data
EXPOSE 8787
USER node
CMD ["node", "server.js"]
