// Monte-Carlo estimate of story pacing for ONE solo player, mirroring HorrorDirector's rules.
// Usage: node tools/simulate_pacing.js [intensity=1.0] [runs=400]
// Keep EVENTS / THRESHOLDS in sync with the Java code when rebalancing.
const intensity = parseFloat(process.argv[2] || '1.0');
const RUNS = parseInt(process.argv[3] || '400', 10);
const THRESHOLDS = [150, 350, 600, 850];     // Stage.java
const GRACE_MIN = 3 * 20;                     // graceDays=3, 20 real minutes per day
const INTERVAL_MIN = 1;                       // directorIntervalSeconds=60
const SUCCESS = 0.75;                         // share of events that find a valid spot

// [id, minStage, weight (typical context), cooldown minutes, dreadGain]
const EVENTS = [
  ['torch_theft', 1, 10, 5, 15], ['sign', 1, 6, 10, 20], ['chest_gift', 1, 5, 15, 25], ['tunnel', 1, 3, 20, 20],
  ['stalk', 2, 10, 6, 20], ['glance', 2, 3, 15, 35], ['phantom_sound', 2, 8, 3, 10], ['hud_flicker', 2, 4, 8, 8],
  ['fog', 2, 4, 25, 25], ['watcher', 2, 2, 10, 20], ['window_title', 2, 2, 15, 5],
  ['contact_join', 3, 7, 20, 30], ['contact_message', 3, 6, 2, 8], ['fake_loading', 3, 3, 20, 20], ['file_drop', 3, 3, 30, 20],
  ['creep', 4, 3, 12, 40], ['system_message', 4, 4, 10, 15], ['fake_disconnect', 4, 2, 40, 30], ['fake_error', 4, 3, 20, 20],
  ['sound_swap', 4, 3, 15, 15], ['hollow', 4, 3, 15, 40], ['the_call', 5, 6, 30, 0],
];

const stageOf = (min, g) => min < GRACE_MIN ? 0 : 1 + THRESHOLDS.filter(t => g >= t).length;
const scaled = v => { const x = v * intensity, w = Math.floor(x); return w + (Math.random() < x - w ? 1 : 0); };

const reach = [[], [], [], [], [], []];
for (let run = 0; run < RUNS; run++) {
  let g = 0, lastEvent = -1e9; const last = {}; const seen = new Set();
  for (let min = 0; min < 20 * 60; min += INTERVAL_MIN) {
    const st = stageOf(min, g);
    if (!seen.has(st)) { seen.add(st); reach[st].push(min); }
    if (st === 0) continue;
    g += scaled(1);
    if (Math.random() > Math.min(0.95, (0.25 + 0.08 * st) * intensity)) continue;
    if (min - lastEvent < 2 * INTERVAL_MIN / intensity) continue;
    const pool = EVENTS.filter(([id, s, , cd]) => s <= st && min - (last[id] ?? -1e9) >= cd / intensity);
    if (!pool.length) continue;
    let r = Math.random() * pool.reduce((a, e) => a + e[2], 0), pick = pool[0];
    for (const e of pool) { r -= e[2]; if (r < 0) { pick = e; break; } }
    if (Math.random() > SUCCESS) continue;
    last[pick[0]] = min; lastEvent = min;
    g += scaled(Math.max(1, Math.floor(pick[4] / 3)));
  }
}
const fmt = m => m === undefined ? '  never' : `${String(Math.floor(m / 60)).padStart(2)}h ${String(Math.round(m % 60)).padStart(2, '0')}m`;
const med = a => { if (!a.length) return undefined; const s = [...a].sort((x, y) => x - y); return s[Math.floor(s.length / 2)]; };
const names = ['silence', 'traces', 'watching', 'contact', 'hostility', 'finale'];
console.log(`intensity ${intensity}, ${RUNS} runs — median real play time to reach each stage (solo):`);
for (let s = 1; s <= 5; s++) console.log(`  stage ${s} ${names[s].padEnd(10)} ${fmt(med(reach[s]))}   (reached in ${Math.round(100 * reach[s].length / RUNS)}% of 20h runs)`);
