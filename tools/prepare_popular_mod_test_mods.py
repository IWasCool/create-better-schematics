"""Download the pinned NeoForge compatibility fixtures into an isolated build directory.

Uses official Modrinth CDN URLs and verifies SHA-512 before accepting a JAR.
No installed Minecraft instance or production dependency is changed.
"""

import argparse
import concurrent.futures
import hashlib
import json
from pathlib import Path
import urllib.request


LOCK = Path(__file__).with_name('popular-mod-test-lock.json')


def prepare(entry, output):
    filename = entry['filename']
    if Path(filename).name != filename or not filename.endswith('.jar'):
        raise ValueError(f'Invalid locked filename: {filename}')
    destination = output / filename
    if destination.exists() and hashlib.sha512(destination.read_bytes()).hexdigest() == entry['sha512']:
        return entry
    request = urllib.request.Request(entry['url'], headers={
        'User-Agent': 'OurCreationStudios/CreateBetterSchematics-compat-tests',
    })
    with urllib.request.urlopen(request, timeout=60) as response:
        data = response.read()
    if hashlib.sha512(data).hexdigest() != entry['sha512']:
        raise ValueError(f'Checksum mismatch: {filename}')
    destination.write_bytes(data)
    return entry


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=Path('build/popular-mod-test-mods'))
    args = parser.parse_args()
    lock = json.loads(LOCK.read_text(encoding='utf-8'))
    entries = lock['files']
    expected = {entry['filename'] for entry in entries}
    stale = {path.name for path in args.output.glob('*.jar')} - expected
    if stale:
        parser.error(f'Output contains other JARs; choose a fresh directory: {sorted(stale)}')
    args.output.mkdir(parents=True, exist_ok=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        prepared = list(pool.map(lambda entry: prepare(entry, args.output), entries))
    (args.output / 'manifest.json').write_text(json.dumps({
        'minecraft': lock['minecraft'], 'loader': lock['loader'], 'files': prepared,
    }, indent=2) + '\n', encoding='utf-8')
    print(f'Prepared {len(prepared)} verified JARs in {args.output.resolve()}')


if __name__ == '__main__':
    main()
