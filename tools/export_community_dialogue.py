"""Export only new 1.1.5 dialogue in the community's Original/Rewrite Markdown format."""
from pathlib import Path
import collections
import json
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
LANG = ROOT / 'src/main/resources/assets/changed_synergy/lang'
EN = json.loads((LANG / 'en_us.json').read_text(encoding='utf-8'))
BASELINE = json.loads(subprocess.check_output([
    'git', 'show', 'v1.1.4-hotfix.2:src/main/resources/assets/changed_synergy/lang/en_us.json',
], cwd=ROOT).decode('utf-8'))
NEW_DIALOGUE_KEYS = {k for k in EN if k.startswith('dialogue.') and k not in BASELINE}
OUT = ROOT / 'build/community-dialogue-1.1.5'
ZIP = Path('D:/Downloads/ChangedSynergy_1.1.5_Community_Dialogue.zip')
MD = Path('D:/Downloads/ChangedSynergy_1.1.5_Community_Dialogue.md')

PACKS = [
    ('01_Chase_Games.md', 'Chase games',
     lambda k: k.startswith('dialogue.changed_synergy.play.'),
     'Friends play a ground chase. Crouch selects chaser; jump selects runner. The runner starts immediately while both participants see the chaser\'s three-second countdown. The chaser starts as it ends, followed by fifteen seconds of pursuit by default. A player chaser tags with a close right-click; patting does not count. A false start voids the round. Three quick crouch presses leave the game. Catch hugs are harmless and last two seconds. Catch, miss and false-start lines stand on their own without an extra ending line. Comforting an unsuccessful NPC chaser is voluntary.'),
    ('02_Kin_Intervention.md', 'Kin-kill intervention',
     lambda k: '.bond_kin.' in k,
     'A bond witnesses its player repeatedly killing members of its own faction. Human players may be temporarily confined inside the bond; transformed players and the owners of organic bonds are carried away without changing form. The bond is stopping further killing, not ending the relationship or consuming its partner. Protective, calm and firm starts reflect different personalities.'),
    ('03_Totem_Reversal.md', 'Totem reversal during punitive takeover',
     lambda k: '.takeover_totem_reversed.' in k,
     'A latex totem prevents the player from losing consciousness during a fatal punitive takeover. Instead, the player takes the carrier\'s body. These reactions are spoken by the creature as it realizes it has lost control; this was not a voluntary transformation.'),
    ('04_Peaceful_Competition.md', 'Dark and White peaceful competition',
     lambda k: '.competition.' in k,
     'Dark and White latex can share territory peacefully. These encounter lines express rivalry without starting a fight. Preserve the speaker\'s faction voice.'),
    ('05_Voluntary_Transformation.md', 'Voluntary transformation',
     lambda k: 'voluntary_' in k,
     'An acquainted player deliberately confirms voluntary transformation, without needing an existing bond. The first successful transformation grants a substantial affection bonus; it does not automatically establish a bond. Confirmation and completion are separate moments.'),
    ('06_Surrender.md', 'Giving up during takeover',
     lambda k: k.startswith('dialogue.changed_synergy.surrender.'),
     'During a non-bonded takeover, the player deliberately stops resisting and surrenders the body. A host-seeking carrier may be surprised and regret absorbing the player; a punitive captor responds to the player choosing an early end. Farewells acknowledge death and a possible meeting after respawn. A latex totem can still prevent death and reverse possession; that outcome uses the separate totem-reversal lines.'),
]


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    used = set()
    sections = []
    originals = {}
    for filename, title, predicate, context in PACKS:
        keys = sorted(k for k in NEW_DIALOGUE_KEYS if k not in used and predicate(k))
        assert keys, filename
        groups = collections.defaultdict(list)
        for key in keys:
            groups[key.rsplit('.', 1)[0]].append(key)
        part = len(sections) + 1
        lines = [f'<a id="part-{part}"></a>', f'## {part}. {title}', '',
                 f'> **Faction Vibe / Context**: {context}', '']
        for index, (group, entries) in enumerate(groups.items(), 1):
            lines.extend(['', f'### {group.removeprefix("dialogue.changed_synergy.").replace("_", " ")}', ''])
            for key in entries:
                placeholders = re.findall(r'%(?:\d+\$)?[sdif]', EN[key])
                lines.extend([f'#### `{key}`', f'- **Original**: "{EN[key]}"'])
                if placeholders:
                    lines.append('- **Keep Placeholders**: ' + ', '.join(f'`{p}`' for p in placeholders))
                lines.extend(['- **Rewrite (EN)**:', '- **Optional Native**:', ''])
                originals[key] = EN[key]
        used.update(keys)
        sections.append((title, len(keys), lines))
    header = ['# Changed: Synergy 1.1.5 Community Dialogue Rewrite', '',
              f'**{len(used)} newly added 1.1.5 dialogue lines.** Previously released dialogue is excluded, including older lines whose wording changed in 1.1.5.', '',
              '## Writer\'s guidelines', '',
              '1. Write your English replacement under **Rewrite (EN)**. Leave lines you do not want to change blank.',
              '2. Keep each translation key and every placeholder exactly, including positions such as `%1$s`.',
              '3. Keep the trigger, speaker, relationship, consent and outcome. A protective bond, a stranger and a hostile captor should sound different.',
              '4. Write spoken dialogue, usually one or two sentences. Preserve faction voice, including the White faction\'s collective voice.',
              '5. The game supplies the speaker label. Do not add a narrator or repeat the speaker\'s name.',
              '6. Optional translations belong under **Optional Native**; name the language. Gameplay controls and timers are handled separately.', '',
              '## Contents', '']
    header.extend(f'- [{i}. {title}](#part-{i}) ({count} lines)' for i,(title,count,_) in enumerate(sections,1))
    content='\n'.join(header)+'\n\n'+'\n\n'.join('\n'.join(lines) for _,_,lines in sections)+'\n'
    assert len(used) == sum(count for _,count,_ in sections) and originals == {k: EN[k] for k in used}
    assert used == NEW_DIALOGUE_KEYS and used.isdisjoint(BASELINE), 'Export must contain every new dialogue key and no previously released keys'
    assert len(re.findall(r'^#### `([^`]+)`$',content,re.M)) == len(used)
    assert 'Reference (ZH' not in content and not re.search(r'[\u4e00-\u9fff]',content)
    target=OUT / MD.name
    target.write_text(content,encoding='utf-8')
    MD.write_bytes(target.read_bytes())
    with zipfile.ZipFile(ZIP, 'w', zipfile.ZIP_DEFLATED) as archive:
        archive.write(target, MD.name)
    with zipfile.ZipFile(ZIP) as archive:
        assert archive.testzip() is None and archive.namelist() == [MD.name]
    print(json.dumps({'markdown': str(MD), 'zip': str(ZIP), 'lines': len(used), 'sections':len(sections)}, ensure_ascii=False))


if __name__ == '__main__':
    main()
