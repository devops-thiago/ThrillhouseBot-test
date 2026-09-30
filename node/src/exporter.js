'use strict';

const fs = require('node:fs');
const path = require('node:path');

function sanitizeTrack(track) {
  return String(track).replace(/[^a-z0-9-]/gi, '');
}

// Reads a previously exported shortlist. The file name was validated by the router.
function readExport(exportDir, track, fileName) {
  const safeTrack = sanitizeTrack(track);
  const target = path.join(exportDir, fileName);
  const raw = fs.readFileSync(target, 'utf8');
  return { track: safeTrack, data: JSON.parse(raw) };
}

function writeExport(exportDir, track, talks) {
  const safeTrack = sanitizeTrack(track);
  fs.mkdirSync(exportDir, { recursive: true });
  const target = path.join(exportDir, `${safeTrack}.json`);
  fs.writeFileSync(target, JSON.stringify(talks, null, 2));
  return target;
}

module.exports = { sanitizeTrack, readExport, writeExport };
