// Writes a 1x1x1 empty structure template (gzipped NBT) used by the mod's game tests.
// Usage: node tools/gen_empty_structure.js
const fs = require('fs'), zlib = require('zlib'), path = require('path');
const OUT = path.join(__dirname, '..', 'src', 'main', 'resources', 'data', 'hollowsignal', 'structure', 'empty.nbt');

const parts = [];
const u8 = v => parts.push(Buffer.from([v]));
const i16 = v => { const b = Buffer.alloc(2); b.writeInt16BE(v); parts.push(b); };
const i32 = v => { const b = Buffer.alloc(4); b.writeInt32BE(v); parts.push(b); };
const str = s => { const b = Buffer.from(s, 'utf8'); i16(b.length); parts.push(b); };
const named = (type, name) => { u8(type); str(name); };
// TAG ids: 0 End, 3 Int, 8 String, 9 List, 10 Compound
named(10, '');
  named(3, 'DataVersion'); i32(3955); // 1.21.1
  named(9, 'size'); u8(3); i32(3); i32(1); i32(1); i32(1);
  named(9, 'palette'); u8(10); i32(1);
    named(8, 'Name'); str('minecraft:air');
    u8(0);
  named(9, 'blocks'); u8(10); i32(1);
    named(9, 'pos'); u8(3); i32(3); i32(0); i32(0); i32(0);
    named(3, 'state'); i32(0);
    u8(0);
  named(9, 'entities'); u8(0); i32(0);
u8(0);
fs.mkdirSync(path.dirname(OUT), { recursive: true });
fs.writeFileSync(OUT, zlib.gzipSync(Buffer.concat(parts)));
console.log('wrote', OUT);
