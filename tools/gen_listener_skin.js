// Generates the original Listener skin (64x64, standard player layout): a faceless figure
// in a long charcoal coat; the face is dark TV static with two faint pale points.
// Usage: node tools/gen_listener_skin.js <out.png>
const fs = require('fs'), zlib = require('zlib');
const W = 64, H = 64, px = new Uint8Array(W * H * 4);
let seed = 1337; const rnd = () => (seed = (seed * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff;
const set = (x, y, r, g, b, a = 255) => { const i = (y * W + x) * 4; px[i] = r; px[i + 1] = g; px[i + 2] = b; px[i + 3] = a; };
const fill = (x0, y0, x1, y1, f) => { for (let y = y0; y < y1; y++) for (let x = x0; x < x1; x++) f(x, y); };
const tone = (base, v) => { const n = Math.round(base + (rnd() - 0.5) * v); return [n, n, n + 3]; };

// Head (all faces) — dark static
fill(0, 0, 32, 16, (x, y) => { const [r, g, b] = tone(28, 16); set(x, y, r, g, b); });
// Face front (8..16, 8..16) — brighter static, "no face"
fill(8, 8, 16, 16, (x, y) => { const v = Math.round(40 + rnd() * 60); set(x, y, v, v, v + 4); });
set(10, 12, 225, 225, 230); set(13, 12, 225, 225, 230); // two faint points where eyes should be
// Body, right leg, right arm (y 16..32) — charcoal coat
fill(0, 16, 56, 32, (x, y) => { const [r, g, b] = tone(22, 10); set(x, y, r, g, b); });
// Coat front seam (body front: x 20..28, y 20..32)
fill(23, 20, 25, 32, (x, y) => set(x, y, 12, 12, 14));
// Left leg (16..32, 48..64) and left arm (32..48, 48..64)
fill(16, 48, 48, 64, (x, y) => { const [r, g, b] = tone(22, 10); set(x, y, r, g, b); });
// Hands: bottom 3 px of arm fronts, pale grey
fill(44, 29, 48, 32, (x, y) => set(x, y, 150, 150, 155));
fill(36, 61, 40, 64, (x, y) => set(x, y, 150, 150, 155));

const crcT = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
const crc = b => { let c = 0xffffffff; for (const x of b) c = crcT[(c ^ x) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length); const td = Buffer.concat([Buffer.from(type), data]); const c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([len, td, c]); };
const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(W, 0); ihdr.writeUInt32BE(H, 4); ihdr[8] = 8; ihdr[9] = 6;
const raw = Buffer.alloc((W * 4 + 1) * H); for (let y = 0; y < H; y++) { raw[y * (W * 4 + 1)] = 0; Buffer.from(px.buffer, y * W * 4, W * 4).copy(raw, y * (W * 4 + 1) + 1); }
fs.writeFileSync(process.argv[2], Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]));
console.log('wrote', process.argv[2]);
