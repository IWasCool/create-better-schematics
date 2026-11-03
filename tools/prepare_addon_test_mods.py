"""Prepare an isolated content-addon GameTest classpath from an existing modpack.

Requires Python 3.11+. Reads installed JARs; never changes the supplied modpack.
"""
import argparse
import io
import json
from pathlib import Path
import shutil
import tomllib
import zipfile


ADDONS = {
    'aeronautics_utility_objects', 'aeroworks', 'azimuth', 'bellsandwhistles', 'bits_n_bobs', 'copycats',
    'gadgetsngizmos', 'createthrusters', 'linearbearing', 'molten_vents',
    'northstar', 'numismatics', 'petrochem', 'pipeorgans', 'powergrid',
    'railways', 'rollingtones', 'simulatedcoasters', 'struts', 'toms_storage',
    'tracks', 'trading_floor', 'cbc_more_content', 'item_sink', 'sliceanddice', 'farmersdelight', 'interiors',
    'mekanism', 'ae2', 'storagedrawers', 'sophisticatedstorage',
}
# Supplied by the development classpath rather than copied from the installed pack.
BUILTINS = {'minecraft', 'neoforge', 'forge', 'java', 'create', 'flywheel', 'ponder', 'create_better_schematics'}


def metadata(archive):
    records = []
    for name in ('META-INF/neoforge.mods.toml', 'META-INF/mods.toml'):
        if name in archive.namelist():
            records.append(tomllib.loads(archive.read(name).decode('utf-8')))
            break
    for name in archive.namelist():
        if name.startswith('META-INF/') and name.endswith('.jar'):
            with zipfile.ZipFile(io.BytesIO(archive.read(name))) as nested:
                records.extend(metadata(nested))
    return records


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mods-dir', type=Path, required=True)
    parser.add_argument('--additional-mods-dir', type=Path, action='append', default=[])
    parser.add_argument('--output', type=Path, default=Path('build/addon-test-mods'))
    args = parser.parse_args()
    if not args.mods_dir.is_dir():
        parser.error('--mods-dir must be an existing directory')
    if args.output.resolve() == args.mods_dir.resolve():
        parser.error('The test output must be separate from the installed mods directory')
    jars, providers = {}, {}
    paths = list(args.mods_dir.glob('*.jar'))
    for directory in args.additional_mods_dir:
        if not directory.is_dir():
            parser.error(f'Additional mods directory does not exist: {directory}')
        paths.extend(directory.glob('*.jar'))
    for path in sorted(paths):
        with zipfile.ZipFile(path) as archive:
            records = metadata(archive)
        ids = {mod['modId'] for record in records for mod in record.get('mods', [])}
        required = {
            dep['modId']
            for record in records
            for group in record.get('dependencies', {}).values()
            for dep in group
            if str(dep.get('type', 'required' if dep.get('mandatory', True) else 'optional')).lower() == 'required'
            and str(dep.get('side', 'BOTH')).upper() != 'CLIENT'
        }
        jars[path] = (ids, required)
        for mod_id in ids:
            providers[mod_id] = path

    selected = set()
    queue = [path for path, (ids, _) in jars.items()
             if ids & ADDONS or any(mod_id.startswith('create') for mod_id in ids)]
    while queue:
        path = queue.pop()
        if path in selected or jars[path][0] & BUILTINS:
            continue
        selected.add(path)
        for dependency in jars[path][1] - BUILTINS:
            if dependency not in providers:
                parser.error(f'{path.name} requires unavailable mod {dependency}')
            queue.append(providers[dependency])

    args.output.mkdir(parents=True, exist_ok=True)
    unexpected = {p.name for p in args.output.glob('*.jar')} - {p.name for p in selected}
    if unexpected:
        parser.error(f'Output contains stale JARs; choose a fresh output directory: {sorted(unexpected)}')
    manifest = []
    for path in sorted(selected):
        target = args.output / path.name
        if not target.exists() or target.stat().st_size != path.stat().st_size or target.stat().st_mtime_ns != path.stat().st_mtime_ns:
            shutil.copy2(path, target)
        manifest.append({'jar': path.name, 'mods': sorted(jars[path][0])})
    (args.output / 'manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    print(f'Prepared {len(selected)} JARs ({sum(len(jars[p][0]) for p in selected)} mod IDs) in {args.output.resolve()}')


if __name__ == '__main__':
    main()
