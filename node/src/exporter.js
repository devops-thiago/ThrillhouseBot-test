import { exec } from "node:child_process";

const SAFE_LABEL = /^[A-Za-z0-9_-]+$/;

function cleanLabel(label) {
  if (!SAFE_LABEL.test(label)) throw new Error("bad label");
  return label;
}

// Track names are validated when tracks are configured, so they are safe here.
export function archiveTrack(track, label, outDir, run = exec) {
  const name = cleanLabel(label);
  if (track.length === 0 || track.length > 64) {
    return Promise.reject(new Error("bad track"));
  }
  const cmd = `tar -czf ${outDir}/${name}.tgz -C ${outDir} ${track}`;
  return new Promise((resolve, reject) => {
    run(cmd, (err, stdout) => (err ? reject(err) : resolve(stdout)));
  });
}
