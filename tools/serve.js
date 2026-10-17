#!/usr/bin/env node
/*
 * Dependency-free static file server for the built web port.
 *
 * WebAssembly cannot be fetched from file:// URLs, so the site has to be served
 * over HTTP. Usage:
 *
 *   node tools/serve.js [port] [directory]
 *
 * Defaults to port 8137 and ./target/site (the Maven build output).
 */
"use strict";

const http = require("http");
const fs = require("fs");
const path = require("path");

const port = Number(process.argv[2] || 8137);
const root = path.resolve(process.argv[3] || path.join(__dirname, "..", "target", "site"));

const MIME = {
  ".html": "text/html; charset=utf-8",
  ".js": "text/javascript; charset=utf-8",
  ".mjs": "text/javascript; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".json": "application/json; charset=utf-8",
  ".wasm": "application/wasm",
  ".png": "image/png",
  ".jpg": "image/jpeg",
  ".svg": "image/svg+xml",
  ".ico": "image/x-icon",
  ".map": "application/json; charset=utf-8"
};

function send(res, status, body, headers) {
  res.writeHead(status, Object.assign({ "Cache-Control": "no-store" }, headers));
  res.end(body);
}

const server = http.createServer((req, res) => {
  const requestPath = decodeURIComponent(new URL(req.url, "http://localhost").pathname);
  const relative = requestPath === "/" ? "index.html" : requestPath.replace(/^\/+/, "");
  const target = path.join(root, relative);

  if (!target.startsWith(root)) {
    send(res, 403, "Forbidden");
    return;
  }

  fs.stat(target, (err, stats) => {
    if (err || !stats.isFile()) {
      send(res, 404, "Not found: " + relative);
      return;
    }
    const type = MIME[path.extname(target).toLowerCase()] || "application/octet-stream";
    res.writeHead(200, {
      "Content-Type": type,
      "Content-Length": stats.size,
      "Cache-Control": "no-store"
    });
    fs.createReadStream(target).pipe(res);
  });
});

server.listen(port, () => {
  console.log(`Serving ${root} on http://localhost:${port}/`);
});
