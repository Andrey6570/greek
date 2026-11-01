#!/usr/bin/env python3
"""Собирает data/deck_extra.js из src/parts/*.json (запуск: python3 -I tools/build.py)."""
import json, glob, re, os
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
out = {'phrases': [], 'cloze': [], 'texts': [], 'rules': []}
for g in sorted(glob.glob(os.path.join(ROOT, 'src/parts/part_*.json'))):
    p = json.load(open(g, encoding='utf8'))
    for k in out: out[k] += p.get(k, [])
for k in out:
    seen = set(); keep = []
    for x in out[k]:
        if x['id'] in seen: print('DROP dup id', x['id']); continue
        seen.add(x['id']); keep.append(x)
    out[k] = keep
seen = {}; keep = []
for p in out['phrases']:
    key = p['ru'].strip().lower()
    if key in seen: print('DROP dup ru', p['id'], seen[key]); continue
    seen[key] = p['id']; keep.append(p)
out['phrases'] = keep
def chk(s, where):
    for g in re.findall(r'\{\{(.+?)\}\}', s):
        o = g.split('|'); assert len(o) == 3 and len(set(o)) == 3, (where, g)
for c in out['cloze'] + out['texts']: chk(c['s'], c['id'])
for r in out['rules']:
    for q in r['quiz']: assert len(set(q['o'])) == 3 and 0 <= q['a'] < 3, (r['id'], q)
    for d in r.get('drill', []): chk(d['s'], r['id'])
open(os.path.join(ROOT, 'data/deck_extra.js'), 'w', encoding='utf8').write('window.EXTRA=' + json.dumps(out, ensure_ascii=False, separators=(',', ':')) + ';\n')
nd = sum(1 for r in out['rules'] if r.get('drill'))
print({k: len(v) for k, v in out.items()}, len({p['l'] for p in out['phrases']}), 'lessons;', nd, 'rules with drill')
