// Source of truth for the entity's dialogue. Generates:
//   data/hollowsignal/dialogue/main.json  (topics -> translation keys)
//   and merges the lines into lang/en_us.json + lang/ru_ru.json
// Usage: node tools/gen_dialogue.js
const fs = require('fs'), path = require('path');
const R = path.join(__dirname, '..', 'src', 'main', 'resources');

// [id, keywords, rel_delta, { band: [[en, ru], ...] }]  — order matters: first keyword match wins.
const topics = require("./dialogue_topics.js");

const json = { topics: [] }, en = {}, ru = {};
for (const [id, keywords, rel, bands] of topics) {
  const replies = {};
  for (const [band, lines] of Object.entries(bands)) {
    replies[band] = lines.map(([e, r], i) => {
      const key = `hollowsignal.dialogue.${id}.${band}.${i}`;
      en[key] = e; ru[key] = r; return key;
    });
  }
  const t = { id, rel_delta: rel, replies };
  if (keywords.length) t.keywords = keywords;
  json.topics.push(t);
}
const dir = path.join(R, 'data', 'hollowsignal', 'dialogue');
fs.mkdirSync(dir, { recursive: true });
fs.writeFileSync(path.join(dir, 'main.json'), JSON.stringify(json, null, 2) + '\n');
for (const [f, add] of [['en_us', en], ['ru_ru', ru]]) {
  const p = path.join(R, 'assets', 'hollowsignal', 'lang', f + '.json');
  const j = JSON.parse(fs.readFileSync(p, 'utf8'));
  for (const k of Object.keys(j)) if (k.startsWith('hollowsignal.dialogue.')) delete j[k];
  Object.assign(j, add);
  fs.writeFileSync(p, JSON.stringify(j, null, 2) + '\n');
}
console.log(`topics: ${json.topics.length}, lines: ${Object.keys(en).length}`);
