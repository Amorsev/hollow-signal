// Generates the mod logo / page icon (512x512 and the in-jar copy), original pixel art:
// a faceless figure made of static, two pale points for eyes, on a dark noisy screen with signal tears.
// Usage: node tools/gen_logo.js
const fs = require('fs'), zlib = require('zlib'), path = require('path');
const ROOT = path.join(__dirname, '..');

function png(file, W, H, px) {
  const crcT = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
  const crc = b => { let c = 0xffffffff; for (const x of b) c = crcT[(c ^ x) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
  const chunk = (t, d) => { const l = Buffer.alloc(4); l.writeUInt32BE(d.length); const td = Buffer.concat([Buffer.from(t), d]); const c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([l, td, c]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(W, 0); ihdr.writeUInt32BE(H, 4); ihdr[8] = 8; ihdr[9] = 6;
  const raw = Buffer.alloc((W * 4 + 1) * H);
  for (let y = 0; y < H; y++) Buffer.from(px.buffer, y * W * 4, W * 4).copy(raw, y * (W * 4 + 1) + 1);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]));
  console.log('wrote', file);
}

// Draw on a 64x64 "pixel" canvas, then upscale ×8 → 512.
const N = 64;
let seed = 2026; const rnd = () => (seed = (seed * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff;
const c = Array.from({ length: N * N }, () => [0, 0, 0]);
const set = (x, y, r, g, b) => { if (x >= 0 && y >= 0 && x < N && y < N) c[y * N + x] = [r, g, b]; };

// background: dark static + vignette
for (let y = 0; y < N; y++) for (let x = 0; x < N; x++) {
  const d = Math.hypot(x - 31.5, y - 31.5) / 45;
  const v = Math.round((8 + rnd() * 38) * (1 - Math.min(1, d) * 0.85));
  set(x, y, v, v, v + 3);
}
// shoulders / coat
for (let y = 46; y < 64; y++) for (let x = 0; x < N; x++) {
  const half = 10 + (y - 46) * 1.4;
  if (Math.abs(x - 31.5) < half) { const v = 14 + Math.round(rnd() * 8); set(x, y, v, v, v + 2); }
}
// head: 24x24 block of brighter static, slightly darker border
for (let y = 18; y < 42; y++) for (let x = 20; x < 44; x++) {
  const edge = x === 20 || x === 43 || y === 18 || y === 41;
  const v = edge ? 18 : 45 + Math.round(rnd() * 70);
  set(x, y, v, v, v + 4);
}
// neck
for (let y = 42; y < 46; y++) for (let x = 27; x < 37; x++) set(x, y, 20, 20, 22);
// eyes: two pale 2x2 points with a faint glow
for (const ex of [26, 36]) {
  for (let dy = -1; dy <= 2; dy++) for (let dx = -1; dx <= 2; dx++) set(ex + dx, 28 + dy, 140, 140, 150);
  for (let dy = 0; dy < 2; dy++) for (let dx = 0; dx < 2; dx++) set(ex + dx, 28 + dy, 238, 238, 245);
}
// horizontal signal tears with red/cyan split
for (const ty of [12, 33, 52]) {
  const shift = Math.round((rnd() - 0.5) * 6);
  for (let x = 0; x < N; x++) {
    const [r, g, b] = c[ty * N + x];
    set(x, ty, Math.min(255, r + 60), g, b);
    const xs = (x + shift + N) % N;
    const [r2, g2, b2] = c[(ty + 1) * N + xs];
    set(xs, ty + 1, r2, Math.min(255, g2 + 50), Math.min(255, b2 + 60));
  }
}

function render(scale) {
  const W = N * scale, px = new Uint8Array(W * W * 4);
  for (let y = 0; y < W; y++) for (let x = 0; x < W; x++) {
    const [r, g, b] = c[Math.floor(y / scale) * N + Math.floor(x / scale)];
    const scan = (y % scale === scale - 1) ? 0.85 : 1; // faint scanlines
    const i = (y * W + x) * 4;
    px[i] = r * scan; px[i + 1] = g * scan; px[i + 2] = b * scan; px[i + 3] = 255;
  }
  return [W, px];
}
let [W, px] = render(8);
png(path.join(ROOT, 'docs', 'icon.png'), W, W, px);                                     // Modrinth / CurseForge icon (512)
[W, px] = render(4);
png(path.join(ROOT, 'src', 'main', 'resources', 'hollowsignal_logo.png'), W, W, px);    // in-jar logo (256)
