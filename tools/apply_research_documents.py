"""Apply the reviewed structure prose, including on later template reimports (nbtlib)."""
from pathlib import Path
import gzip
import hashlib
import io
import json

import nbtlib as nbt

ROOT = Path(__file__).resolve().parents[1]
DOCUMENTS = json.loads((Path(__file__).with_name('research_structure_documents.json')).read_text(encoding='utf-8'))['documents']


def apply_documents(template, name):
    count = 0
    for document in DOCUMENTS:
        if document['structure'] != name or 'loot_table' in document:
            continue
        entity = document['holder'].endswith('item_frame')
        candidates = template['entities' if entity else 'blocks']
        matches = [record for record in candidates if list(map(float, record['pos'])) == document['position']]
        assert len(matches) == 1, (document['id'], 'document holder moved or missing')
        record = matches[0]
        kind = str(record['nbt']['id']) if entity else str(template['palette'][int(record['state'])]['Name'])
        assert kind == document['holder'], (document['id'], kind)
        target = record['nbt']
        for key in document.get('item_path', []):
            target = target[int(key)] if key.isdigit() else target[key]
        if 'item_slot' in document:
            items = [item for item in target if int(item['Slot']) == document['item_slot']]
            assert len(items) == 1, (document['id'], 'document slot moved or missing')
            target = items[0]
        if 'field' in document:
            assert len(document['pages']) == 1
            target[document['field']] = nbt.String(document['pages'][0])
        elif 'title' in document:
            assert str(target['id']) == 'minecraft:written_book'
            tag = target['tag']
            assert str(tag['author']) == document['author'], document['id']
            assert str(tag['title']) in (document.get('source_title', document['title']), document['title']), document['id']
            tag['title'] = nbt.String(document['title'])
            tag['pages'] = nbt.List[nbt.String]([json.dumps({'text': page}, ensure_ascii=False) for page in document['pages']])
        else:
            assert str(target['id']) == 'changed:compact_disc'
            target.setdefault('tag', nbt.Compound())['Text'] = nbt.String(document['pages'][0])
        count += 1
    return count


def save(template, path):
    buffer = io.BytesIO()
    template.write(buffer)
    path.write_bytes(gzip.compress(buffer.getvalue(), mtime=0))


def main():
    folder = ROOT / 'src/main/resources/data/changed_synergy'
    manifest_path = ROOT / 'tools/research_structures_manifest.json'
    manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
    for name in manifest:
        path = folder / 'structures' / (name + '.nbt')
        template = nbt.load(path)
        count = apply_documents(template, name)
        save(template, path)
        manifest[name]['output_sha256'] = hashlib.sha256(path.read_bytes()).hexdigest()
        manifest[name]['changes']['reviewed structure documents'] = count
        print(name, count)
    manifest_path.write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    document = next(d for d in DOCUMENTS if 'loot_table' in d)
    path = folder / 'loot_tables/chests/swordfish_research.json'
    data = json.loads(path.read_text(encoding='utf-8'))
    function = data['pools'][1]['entries'][0]['functions'][0]
    tag = nbt.parse_nbt(function['tag'])
    tag['Text'] = nbt.String(document['pages'][0])
    function['tag'] = tag.snbt()
    assert str(nbt.parse_nbt(function['tag'])['Text']) == document['pages'][0]
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
