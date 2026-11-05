// Checks that every translation key the mod uses exists in BOTH en_us and ru_ru, and that both files
// have the same keys. Dynamic keys ("prefix." + i) are checked by expanding known ranges below.
// Usage: node tools/check_lang.js   (exit code 1 on problems)
const fs = require('fs'), path = require('path');
const ROOT = path.join(__dirname, '..');
const LANG = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'hollowsignal', 'lang');
const en = JSON.parse(fs.readFileSync(path.join(LANG, 'en_us.json'), 'utf8'));
const ru = JSON.parse(fs.readFileSync(path.join(LANG, 'ru_ru.json'), 'utf8'));

function walk(dir, out = []) {
  for (const f of fs.readdirSync(dir)) {
    const p = path.join(dir, f);
    if (fs.statSync(p).isDirectory()) walk(p, out); else out.push(p);
  }
  return out;
}

const used = new Set();
const src = walk(path.join(ROOT, 'src', 'main', 'java')).filter(f => f.endsWith('.java'));
for (const f of src) {
  for (const m of fs.readFileSync(f, 'utf8').matchAll(/"(hollowsignal\.[a-z0-9_.]+[a-z0-9_])"/g)) used.add(m[1]);
}
// Config entries and sections: the config GUI looks up hollowsignal.configuration.<name>
const cfg = fs.readFileSync(path.join(ROOT, 'src/main/java/com/hollowsignal/config/HSConfig.java'), 'utf8');
for (const m of cfg.matchAll(/\.(?:define|defineEnum|defineInRange|defineListAllowEmpty|push)\("([A-Za-z_]+)"/g)) {
  used.add('hollowsignal.configuration.' + m[1]);
}
// Dialogue JSON references
const dlg = JSON.parse(fs.readFileSync(path.join(ROOT, 'src/main/resources/data/hollowsignal/dialogue/main.json'), 'utf8'));
for (const t of dlg.topics) for (const list of Object.values(t.replies)) list.forEach(k => used.add(k));
// Dynamic key families: [prefix, count, suffixes]
const families = [
  ['hollowsignal.stage.', 6, ['']], ['hollowsignal.splash.', 16, ['']], ['hollowsignal.error.', 5, ['']],
  ['hollowsignal.disconnect.', 4, ['']], ['hollowsignal.note.', 5, ['']], ['hollowsignal.loading.wrong.', 3, ['']],
  ['hollowsignal.gift.', 8, ['.name', '.lore']], ['hollowsignal.ending.released.', 6, ['']], ['hollowsignal.ending.loop.', 6, ['']],
];
for (const [p, n, sufs] of families) for (let i = 0; i < n; i++) for (const s of sufs) used.add(p + i + s);
// Signs: lines per message, mirrored from SignEvent.MESSAGES
[1, 2, 2, 2, 2, 2, 2, 1, 2, 2].forEach((lines, i) => { for (let l = 0; l < lines; l++) used.add(`hollowsignal.sign.${i}.${l}`); });
// Prefix-only strings in code (e.g. "hollowsignal.sign.") are not keys
for (const k of [...used]) if (k.endsWith('.') || /\.(sign|gift|stage|splash|error|disconnect|note|dialogue)$/.test(k)) used.delete(k);

let problems = 0;
const report = (msg) => { console.log(msg); problems++; };
for (const k of [...used].sort()) {
  if (!(k in en)) report(`missing in en_us: ${k}`);
  if (!(k in ru)) report(`missing in ru_ru: ${k}`);
}
for (const k of Object.keys(en)) if (!(k in ru)) report(`only in en_us: ${k}`);
for (const k of Object.keys(ru)) if (!(k in en)) report(`only in ru_ru: ${k}`);
console.log(problems ? `\n${problems} problem(s)` : `OK: ${used.size} used keys, ${Object.keys(en).length} entries in each language`);
process.exit(problems ? 1 : 0);
