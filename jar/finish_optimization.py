import json
from pathlib import Path

root = Path('C:/Users/MrUser/Desktop/MinecraftDev')
models = root / 'smokemod_main/src/main/resources/assets/smokemod/models/block'
backup = root / 'jar/before_optimization'
for name, shell_count in [('jar', 3), ('jar_large', 6)]:
    path = models / (name + '.json')
    before = json.loads((backup / path.name).read_text(encoding='utf-8-sig'))
    after = json.loads(path.read_text(encoding='utf-8-sig'))
    assert before.get('display') == after.get('display'), name + ': display changed'
    assert before['textures'] == after['textures'], name + ': textures changed'
    for a, b in zip(before['elements'][:shell_count], after['elements'][:shell_count]):
        assert a['from'] == b['from'] and a['to'] == b['to']
        for side, face in b['faces'].items():
            assert face['uv'] == a['faces'][side]['uv']
            assert face['texture'] == a['faces'][side]['texture']
    if name == 'jar':
        for a, b in zip(before['elements'][3:], after['elements'][3:]):
            for axis in range(3):
                margin = (a['to'][axis] - a['from'][axis]) / 8
                assert abs(b['from'][axis] - a['from'][axis] - margin) < 1e-5
                assert abs(b['to'][axis] - a['to'][axis] + margin) < 1e-5
            assert all(f['uv'] == [9, 9, 15, 15] for f in b['faces'].values())
    for e in after['elements']:
        assert all(lo <= hi for lo, hi in zip(e['from'], e['to']))
        e.pop('name', None)
        if e.get('rotation', {}).get('angle') == 0:
            e.pop('rotation')
        for f in e['faces'].values():
            assert f['texture'].lstrip('#') in after['textures']
            if f.get('rotation') == 0:
                f.pop('rotation')
    after.pop('groups', None)
    after.pop('credit', None)
    elements = after.pop('elements')
    header = json.dumps(after, ensure_ascii=False, indent=2)
    output = header[:-2] + ',\n  "elements": [\n' + ',\n'.join('    ' + json.dumps(e, separators=(',', ':')) for e in elements) + '\n  ]\n}\n'
    json.loads(output)
    path.write_text(output, encoding='utf-8')
    print(name, json.dumps({'elements_before': len(before['elements']), 'elements_after': len(elements), 'faces_before': sum(len(e['faces']) for e in before['elements']), 'faces_after': sum(len(e['faces']) for e in elements), 'bytes_before': (backup / path.name).stat().st_size, 'bytes_after': path.stat().st_size, 'lines_after': len(output.splitlines())}))
