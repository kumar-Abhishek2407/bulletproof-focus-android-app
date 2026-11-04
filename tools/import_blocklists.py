"""Import the user's extension rules, preserving exact-only domain semantics."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil

ROOT = Path(__file__).resolve().parents[1]

def build(source):
    suffix, exact = set(), set()
    provenance = []
    for name in ('adult.json', 'adult-additions.json', 'adult-expanded.json'):
        path = source / 'rules' / name
        payload = path.read_bytes()
        for rule in json.loads(payload):
            condition = rule['condition']
            suffix.update(condition.get('requestDomains', []))
            pattern = condition.get('urlFilter')
            if pattern:
                match = re.fullmatch(r'\|https?://([a-z0-9.-]+)\^', pattern)
                if not match:
                    raise ValueError(f'Unsupported rule: {pattern}')
                exact.add(match[1])
        provenance.append({'file': name, 'sha256': hashlib.sha256(payload).hexdigest()})
    for domain in suffix | exact:
        if not re.fullmatch(r'[a-z0-9-]+(?:\.[a-z0-9-]+)*', domain):
            raise ValueError(domain)
    assets = ROOT / 'app/src/main/assets'
    for name, values in [('adult-domains.txt', suffix), ('adult-exact.txt', exact)]:
        payload = ''.join(d + '\n' for d in sorted(values)).encode('ascii')
        (assets / name).write_bytes(payload)
    notices = assets / 'blocklist-notices'
    notices.mkdir(exist_ok=True)
    for name in ('LICENSE.txt', 'SOURCE.json', 'EXPANDED-LICENSE.txt', 'EXPANDED-SOURCE.json'):
        shutil.copyfile(source / 'rules' / name, notices / name)
    metadata = {'suffixDomains': len(suffix), 'exactDomains': len(exact), 'sourceRules': provenance}
    (assets / 'blocklist-metadata.json').write_text(json.dumps(metadata, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(metadata, indent=2))

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('extension', type=Path)
    build(parser.parse_args().extension)
