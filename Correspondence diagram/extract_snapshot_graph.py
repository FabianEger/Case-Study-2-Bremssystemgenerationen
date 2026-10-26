"""Extract instances, references and correspondences of the Case Study 2 V-SUM states into one
JSON graph that correspondence_diagram.html renders:
  - the two initial-state snapshots the scenario tests load (vsum/src/test/resources/...)
  - the final state the scenario-2 test leaves in vsum/target (run `mvn clean verify` first)

Usage:  python3 -I extract_snapshot_graph.py <MT_CaseStudy2-folder> <out.json>
"""
import sys, os, json, re
import xml.etree.ElementTree as ET

SNAP = 'vsum/src/test/resources/casestudy2/snapshots/'
SNAPSHOTS = [
    ('s1', SNAP + 'scenario1-initial-state-gn-1', 'Scenario 1 initial state (Gn-1)'),
    ('s2', SNAP + 'scenario2-initial-state-gn', 'Scenario 2 initial state (Gn)'),
    ('s3', 'vsum/target/casestudy2-scenario2-vsum-output/vsum', 'Final state after scenarios 1 and 2 (Gn+1)'),
]
# file extension -> (view, model kind, containment features in order)
KINDS = {
    'product_requirements': ('product', 'Requirements', ['requirements']),
    'product_functions': ('product', 'Functions', ['functions']),
    'product_architecture': ('product', 'Architecture', ['components']),
    'cameo_requirements': ('ve', 'Requirements', ['requirements']),
    'cameo_functions': ('ve', 'Functions', ['functions']),
    'cameo_architecture': ('ve', 'Architecture', ['blocks', 'configurations']),
}
ATTRS = ['Id', 'name', 'text', 'kind', 'classification', 'status', 'imaginary', 'typeName',
         'generation', 'restVehicleSimulationAffected', 'reviewRequired']
LISTS = ['generations', 'categories']
LOCAL_REFS = ['parent', 'general', 'dependencies', 'references', 'parts']


def parse_model(folder, fname):
    ext = fname.rsplit('.', 1)[1]
    view, kind, feats = KINDS[ext]
    root = ET.parse(os.path.join(folder, fname)).getroot()
    rid = fname + '#/'
    gen = root.get('generation')
    nodes = [dict(id=rid, root=True, feature=None, name=None, attrs={'generation': gen} if gen else {})]
    refs = []
    pred = root.find('predecessor')
    if pred is not None:
        refs.append(dict(kind='predecessor', src=rid, dst=resolve(fname, pred.get('href'))))
    counters = {}
    for ch in root:
        if ch.tag not in feats:
            continue
        i = counters.get(ch.tag, 0)
        counters[ch.tag] = i + 1
        nid = f'{fname}#//@{ch.tag}.{i}'
        attrs = {a: ch.get(a) for a in ATTRS if ch.get(a) is not None}
        if 'text' in attrs and len(attrs['text']) > 400:
            attrs['text'] = attrs['text'][:400] + '…'
        for l in LISTS:
            vals = [c.text for c in ch.findall(l) if c.text]
            if vals:
                attrs[l] = vals
        nodes.append(dict(id=nid, root=False, feature=ch.tag, name=ch.get('name'), attrs=attrs))
        for r in LOCAL_REFS:
            for frag in (ch.get(r) or '').split():
                refs.append(dict(kind=r, src=nid, dst=fname + '#' + frag))
        for c in ch:
            if c.get('href'):
                refs.append(dict(kind=c.tag, src=nid, dst=resolve(fname, c.get('href'))))
    gen_label = gen or ('V87' if view == 've' else None)
    return dict(file=fname, view=view, kind=kind, generation=gen, label=gen_label, nodes=nodes), refs


def resolve(fname, href):
    f, frag = href.split('#', 1)
    return (os.path.basename(f) or fname) + '#' + frag


def parse_snapshot(folder):
    models, refs = [], []
    for f in sorted(os.listdir(folder)):
        if '.' in f and f.rsplit('.', 1)[1] in KINDS:
            m, r = parse_model(folder, f)
            models.append(m)
            refs += r
    corr = []
    root = ET.parse(os.path.join(folder, 'vsum', 'correspondences.correspondence')).getroot()
    for c in root:
        left = [resolve('', e.get('href')) for e in c.findall('leftEObjects')]
        right = [resolve('', e.get('href')) for e in c.findall('rightEObjects')]
        corr.append(dict(tag=c.get('tag'), left=left, right=right))
    return models, refs, corr


if __name__ == '__main__':
    base, out = sys.argv[1], sys.argv[2]
    data = dict(snapshots=[])
    for key, folder, title in SNAPSHOTS:
        models, refs, corr = parse_snapshot(os.path.join(base, folder))
        newest = max(os.path.getmtime(os.path.join(base, folder, f)) for f in os.listdir(os.path.join(base, folder))
                     if os.path.isfile(os.path.join(base, folder, f)))
        import datetime
        data['snapshots'].append(dict(key=key, folder=folder, title=title, models=models, refs=refs, correspondences=corr,
                                      modified=datetime.datetime.fromtimestamp(newest).strftime('%Y-%m-%d %H:%M')))
    json.dump(data, open(out, 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
    for s in data['snapshots']:
        print(s['key'], [(m['file'], len(m['nodes']) - 1) for m in s['models']],
              'refs', len(s['refs']), 'corr', len(s['correspondences']))
