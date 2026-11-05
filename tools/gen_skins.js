// Generates the original 64x64 skins (standard player layout) for the secondary entities.
// Usage: node tools/gen_skins.js
const fs = require('fs'), zlib = require('zlib'), path = require('path');
const OUT = path.join(__dirname, '..', 'src', 'main', 'resources', 'assets', 'hollowsignal', 'textures', 'entity');

function skin(name, seed0, paint) {
  const W = 64, H = 64, px = new Uint8Array(W * H * 4);
  let seed = seed0; const rnd = () => (seed = (seed * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff;
  const set = (x, y, r, g, b, a = 255) => { const i = (y * W + x) * 4; px[i] = r; px[i + 1] = g; px[i + 2] = b; px[i + 3] = a; };
  const fill = (x0, y0, x1, y1, f) => { for (let y = y0; y < y1; y++) for (let x = x0; x < x1; x++) f(x, y); };
  const noisy = (r, g, b, v) => (x, y) => { const n = (rnd() - 0.5) * v; set(x, y, clamp(r + n), clamp(g + n), clamp(b + n)); };
  const clamp = v => Math.max(0, Math.min(255, Math.round(v)));
  // regions of the classic layout
  const R = {
    head: [0, 0, 32, 16], face: [8, 8, 16, 16], upper: [0, 16, 56, 32], lower: [16, 48, 48, 64],
    bodyFront: [20, 20, 28, 32], rArmFront: [44, 20, 48, 32], lArmFront: [36, 52, 40, 64],
  };
  paint({ set, fill, noisy, rnd, R });
  write(path.join(OUT, name + '.png'), W, H, px);
}

function write(file, W, H, px) {
  const crcT = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
  const crc = b => { let c = 0xffffffff; for (const x of b) c = crcT[(c ^ x) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
  const chunk = (t, d) => { const l = Buffer.alloc(4); l.writeUInt32BE(d.length); const td = Buffer.concat([Buffer.from(t), d]); const c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([l, td, c]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(W, 0); ihdr.writeUInt32BE(H, 4); ihdr[8] = 8; ihdr[9] = 6;
  const raw = Buffer.alloc((W * 4 + 1) * H);
  for (let y = 0; y < H; y++) Buffer.from(px.buffer, y * W * 4, W * 4).copy(raw, y * (W * 4 + 1) + 1);
  fs.writeFileSync(file, Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]));
  console.log('wrote', file);
}

// Watcher: tall, pale grey, smooth white face with one black vertical slit.
skin('watcher', 4242, ({ set, fill, noisy, R }) => {
  fill(...R.head, noisy(170, 170, 176, 14));
  fill(...R.face, noisy(232, 232, 236, 8));
  for (let y = 9; y < 15; y++) set(11, y, 8, 8, 10), set(12, y, 8, 8, 10);
  fill(...R.upper, noisy(150, 150, 156, 16));
  fill(...R.lower, noisy(150, 150, 156, 16));
  fill(...R.rArmFront, noisy(120, 120, 126, 10));
  fill(...R.lArmFront, noisy(120, 120, 126, 10));
});

// Reporter: small, near-black, a single red "signal" light where the face should be.
skin('reporter', 777, ({ set, fill, noisy, R }) => {
  fill(...R.head, noisy(24, 24, 28, 10));
  fill(...R.face, noisy(16, 16, 20, 6));
  fill(11, 11, 13, 13, (x, y) => set(x, y, 230, 30, 30));
  set(12, 10, 140, 20, 20); set(10, 12, 140, 20, 20); set(13, 12, 140, 20, 20); set(12, 13, 140, 20, 20);
  fill(...R.upper, noisy(30, 30, 34, 12));
  fill(...R.lower, noisy(30, 30, 34, 12));
  fill(21, 22, 27, 24, (x, y) => set(x, y, 90, 16, 16)); // red stripe across the chest
});

// Hollow: black body with dim purple cracks; the face is a dark void with a thin pale mouth.
skin('hollow', 9001, ({ set, fill, noisy, rnd, R }) => {
  fill(...R.head, noisy(10, 8, 12, 8));
  fill(...R.face, noisy(4, 3, 6, 4));
  for (let x = 9; x < 15; x++) set(x, 14, 200, 196, 210);
  set(10, 11, 120, 60, 150); set(13, 11, 120, 60, 150);
  const crack = (x0, y0, x1, y1) => { // jagged purple veins
    let x = x0, y = y0;
    while (y < y1) { set(x, y, 90, 40, 120); y++; x += Math.round((rnd() - 0.5) * 2); x = Math.max(x0 - 2, Math.min(x1, x)); }
  };
  fill(...R.upper, noisy(14, 12, 16, 10));
  fill(...R.lower, noisy(14, 12, 16, 10));
  crack(22, 20, 26, 32); crack(25, 21, 27, 31); crack(45, 21, 47, 32); crack(4, 21, 6, 32); crack(37, 53, 39, 64); crack(20, 52, 22, 64);
});
