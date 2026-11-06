'use strict';
/**
 * Runs the Cool Client API (app.js) on Node 20+: `node server.js`, or `docker compose up -d`.
 * The same API also runs on Cloudflare Workers, see worker/.
 *
 * Node-only environment variables (the API's own settings are listed in app.js):
 *   PORT         listen port (default 8787)
 *   DATA_DIR     where db.json lives (default ./data)
 *   TRUST_PROXY  "1" behind a reverse proxy (Caddy, nginx) so rate limits use X-Forwarded-For
 */
const http = require('http');
const fs = require('fs');
const path = require('path');
const { createApp, emptyDb, sanitizeLoadout, normUuid, tebexItems, HEADERS, VERSION, MAX_BODY } = require('./app');

function loadDb(file) {
  if (!fs.existsSync(file)) return emptyDb();
  try {
    return Object.assign(emptyDb(), JSON.parse(fs.readFileSync(file, 'utf8')));
  } catch (e) {
    console.error('Could not read db.json, starting fresh:', e.message);
    fs.renameSync(file, file + '.broken-' + Date.now());
    return emptyDb();
  }
}

function readRaw(req) {
  return new Promise((resolve, reject) => {
    let size = 0;
    const chunks = [];
    req.on('data', (c) => {
      size += c.length;
      if (size > MAX_BODY) {
        reject(new Error('Body too large'));
        req.destroy();
        return;
      }
      chunks.push(c);
    });
    req.on('end', () => resolve(Buffer.concat(chunks)));
    req.on('error', reject);
  });
}

const PUBLIC = path.join(__dirname, 'public');
const TYPES = { '.png': 'image/png', '.json': 'application/json' };

/** Serves cosmetic files (public/cosmetics/) at /cosmetics/...; on Cloudflare the worker's static assets do this. */
function serveStatic(req, res) {
  const url = new URL(req.url, 'http://localhost');
  if ((req.method !== 'GET' && req.method !== 'HEAD') || !url.pathname.startsWith('/cosmetics/')) return false;
  let file;
  try {
    file = path.join(PUBLIC, decodeURIComponent(url.pathname));
  } catch (e) {
    file = '';
  }
  const type = TYPES[path.extname(file).toLowerCase()];
  if (!type || !file.startsWith(path.join(PUBLIC, 'cosmetics') + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) {
    send(res, 404, { error: 'Not found' });
    return true;
  }
  const data = fs.readFileSync(file);
  res.writeHead(200, { 'Content-Type': type, 'Content-Length': data.length, 'Access-Control-Allow-Origin': '*', 'Cache-Control': 'public, max-age=300' });
  res.end(req.method === 'HEAD' ? undefined : data);
  return true;
}

function send(res, status, body) {
  const data = JSON.stringify(body);
  res.writeHead(status, Object.assign({ 'Content-Length': Buffer.byteLength(data) }, HEADERS));
  res.end(data);
}

/** Starts the API on `port` (0 = any free port) and resolves with the http.Server. */
function start(port, env) {
  env = env || process.env;
  const dataDir = env.DATA_DIR || path.join(__dirname, 'data');
  fs.mkdirSync(dataDir, { recursive: true });
  const file = path.join(dataDir, 'db.json');
  const trustProxy = env.TRUST_PROXY === '1';

  let saveTimer = null;
  let app = null;
  const writeNow = () => {
    if (saveTimer) clearTimeout(saveTimer);
    saveTimer = null;
    const tmp = file + '.tmp';
    fs.writeFileSync(tmp, JSON.stringify(app.db, null, 2));
    fs.renameSync(tmp, file);
  };
  // db.json is small: write the whole thing, at most twice a second.
  app = createApp({
    env,
    db: loadDb(file),
    persist: () => {
      if (!saveTimer) saveTimer = setTimeout(writeNow, 500);
    },
  });

  const server = http.createServer(async (req, res) => {
    if (serveStatic(req, res)) return;
    let raw;
    try {
      raw = await readRaw(req);
    } catch (e) {
      return send(res, 413, { error: e.message });
    }
    let ip = req.socket.remoteAddress || 'unknown';
    if (trustProxy) {
      const fwd = String(req.headers['x-forwarded-for'] || '').split(',')[0].trim();
      if (fwd) ip = fwd;
    }
    const r = await app.handle({ method: req.method, url: req.url, headers: req.headers, ip, body: raw });
    send(res, r.status, r.body);
  });
  server.flush = () => saveTimer && writeNow();
  return new Promise((resolve) => {
    server.listen(port, () => {
      const flags = (env.AURORA_INSECURE === '1' ? ' (INSECURE dev mode: offline accounts allowed)' : '') + (env.ENFORCE_ENTITLEMENTS === '1' ? ' (entitlements enforced)' : '');
      console.log(`Cool Client API ${VERSION} listening on :${server.address().port}${flags}`);
      resolve(server);
    });
  });
}

if (require.main === module) {
  start(parseInt(process.env.PORT || '8787', 10)).then((server) => {
    // Don't lose the last half second of changes on `docker stop`.
    for (const sig of ['SIGTERM', 'SIGINT']) {
      process.on(sig, () => {
        server.flush();
        process.exit(0);
      });
    }
  });
}

module.exports = { start, sanitizeLoadout, normUuid, tebexItems };
