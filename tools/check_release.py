"""Audit release sources and JARs using only the Python standard library."""

import argparse
from collections import Counter
import glob
import hashlib
import json
from pathlib import Path
import re
import tomllib
import zipfile


ROOT = Path(__file__).resolve().parent.parent
MOD_ID = 'create_better_schematics'
PACKAGE = 'com/ourcreationstudios/' + MOD_ID + '/'


def require(condition, message):
    if not condition:
        raise ValueError(message)


def unique_keys(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, f'Duplicate JSON key: {key}')
        result[key] = value
    return result


def read_json(data):
    return json.loads(data, object_pairs_hook=unique_keys)


def placeholders(value):
    # Minecraft translations support %s, numbered %s arguments and literal %%.
    require(not re.sub(r'%(?:[1-9]\d*\$)?s|%%', '', value).count('%'),
            f'Invalid translation placeholder: {value}')
    implicit = 0
    arguments = []
    for match in re.finditer(r'%(?:([1-9]\d*)\$)?s|%%', value):
        if match.group() == '%%':
            continue
        if match[1]:
            arguments.append(int(match[1]))
        else:
            implicit += 1
            arguments.append(implicit)
    return Counter(arguments)


def check_sources():
    version = re.search(r'^mod_version=(.+)$', (ROOT / 'gradle.properties').read_text(), re.M)[1].strip()
    for loader in ['forge', 'fabric']:
        script = (ROOT / loader / 'build.gradle').read_text()
        require(re.search(r"^version = '" + re.escape(version) + r"'$", script, re.M),
                f'{loader} build version differs from {version}')
    forge = tomllib.loads((ROOT / 'forge/src/main/resources/META-INF/mods.toml').read_text())
    require(forge['mods'][0]['version'] == version, 'Forge metadata version mismatch')

    language_dir = ROOT / f'src/main/resources/assets/{MOD_ID}/lang'
    english = read_json((language_dir / 'en_us.json').read_text(encoding='utf-8'))
    for path in sorted(language_dir.glob('*.json')):
        translations = read_json(path.read_text(encoding='utf-8'))
        require(translations.keys() == english.keys(), f'Language keys differ: {path.name}')
        for key, value in translations.items():
            require(isinstance(value, str) and value.strip() and '\ufffd' not in value,
                    f'Empty or damaged translation: {path.name}: {key}')
            require(placeholders(value) == placeholders(english[key]),
                    f'Placeholder arguments differ: {path.name}: {key}')

    sources = ROOT / f'src/main/java/{PACKAGE}'
    java = {p: p.read_text(encoding='utf-8') for p in sources.rglob('*.java')}
    all_java = '\n'.join(java.values())
    scopes = {
        'schematic.mods': ['client/SchematicModsInfo.java'],
        'cannon.skip': ['client/CannonSkipScreen.java', 'cannon/CannonSkipPackets.java'],
        'cannon.jei': ['client/CannonMaterialsClient.java', 'mixin/client/SchematicannonScreenMixin.java',
                       'cannon/CannonMaterialsPackets.java'],
        'cannon.emi': ['client/CannonMaterialsClient.java', 'mixin/client/SchematicannonScreenMixin.java',
                       'cannon/CannonMaterialsPackets.java'],
    }
    for scope, files in scopes.items():
        text = '\n'.join((sources / f).read_text(encoding='utf-8') for f in files)
        prefix = MOD_ID + '.' + scope + '.'
        strings = set(re.findall(r'"([^"\n]*)"', text))
        expected = set(re.findall(r'(?:\btext|CannonMaterialsClient\.text)\("([^"\n]+)"\s*[,)]', text))
        if scope == 'cannon.jei':
            expected.discard('required')  # Only shown inside the needsEmiAddon() branch.
        for key in expected:
            require(prefix + key in english, f'Missing translation: {prefix}{key}')
        # Error suffixes are generated from packet result enums, not string literals.
        if scope.startswith('cannon.'):
            enum = re.search(r'public enum Result\s*\{([^}]+)}', text)[1]
            for result in re.findall(r'\b[A-Z][A-Z_]+\b', enum):
                if scope == 'cannon.skip' and result == 'CLOSED':
                    continue  # Closes the screen before any status text is rendered.
                if result not in {'READY', 'APPLIED'}:
                    require(prefix + 'error.' + result.lower() in english,
                            f'Missing {scope} error translation: {result}')
        for key in english:
            if key.startswith(prefix):
                suffix = key.removeprefix(prefix)
                require(suffix in strings or suffix.startswith('error.') or key in all_java,
                        f'Unreferenced translation: {key}')
    for suite in ['src/testFixtures', 'src/compatTest', 'src/nativeCompatibilityTest',
                  'forge/src/gametest', 'fabric/src/gametest']:
        for path in (ROOT / suite / 'java').rglob('*.java'):
            for fixture in re.findall(r'"(/fixtures/[^"\n]+\.nbt)"', path.read_text(encoding='utf-8')):
                require((ROOT / suite / 'resources' / fixture.lstrip('/')).is_file(),
                        f'Missing test resource in {suite}: {fixture}')
    print(f'Sources: version {version}; {len(english)} English keys; language keys and placeholders valid')
    return version, english


def check_jar(path, version, english):
    require(path.is_file(), f'Missing JAR: {path}')
    with zipfile.ZipFile(path) as jar:
        names = jar.namelist()
        require(len(names) == len(set(names)), f'Duplicate entries in {path.name}')
        forbidden = re.compile(
            r'(?i)(^|/)(gametest|compat_gametest|client_tests|native_tests|testsupport|fixtures)(/|$)'
            r'|GameTests?[^/]*\.class$|\.(java|log|hprof|jfr|bbmodel|psd|bak|tmp)$'
            r'|(^|/)(\.git|\.idea|__pycache__)(/|$)|/structure/empty\.nbt$')
        unexpected = [name for name in names if forbidden.search(name)]
        require(not unexpected, f'Test/development content in {path.name}: {unexpected}')
        require(not any(name.endswith('.png') for name in names),
                f'Unexpected bundled artwork in {path.name}; this mod uses native textures')
        if 'fabric.mod.json' in names:
            metadata = read_json(jar.read('fabric.mod.json'))
            require(metadata['version'] == version and metadata['id'] == MOD_ID,
                    f'Fabric metadata mismatch: {path.name}')
            require(metadata['mixins'] == ['create_better_schematics_fabric.mixins.json'],
                    'Fabric mixin configuration mismatch')
            bytecode_version = 61
        else:
            metadata_path = ('META-INF/neoforge.mods.toml' if 'META-INF/neoforge.mods.toml' in names
                             else 'META-INF/mods.toml')
            metadata = tomllib.loads(jar.read(metadata_path).decode())
            require(metadata['mods'][0]['version'] == version and metadata['mods'][0]['modId'] == MOD_ID,
                    f'Mod metadata mismatch: {path.name}')
            bytecode_version = 65 if 'neoforge' in metadata_path else 61
            if bytecode_version == 65:
                packaged = read_json(jar.read(f'assets/{MOD_ID}/lang/en_us.json'))
                require(packaged == english, 'Packaged English differs from source')
        for name in names:
            if name.endswith('.mixins.json'):
                config = read_json(jar.read(name))
                classes = [config['package'] + '.' + c
                           for group in ['mixins', 'client', 'server'] for c in config.get(group, [])]
                if config.get('plugin'):
                    classes.append(config['plugin'])
                for cls in classes:
                    require(cls.replace('.', '/') + '.class' in names, f'Missing mixin class: {cls}')
            if name.startswith(PACKAGE) and name.endswith('.class'):
                require(int.from_bytes(jar.read(name)[6:8], 'big') == bytecode_version,
                        f'Wrong Java bytecode version: {name}')
        require(any(name.startswith(PACKAGE) and name.endswith('.class') for name in names),
                f'No mod classes in {path.name}')
    print(f'JAR OK: {path.name} ({path.stat().st_size:,} bytes), SHA-256 {hashlib.sha256(path.read_bytes()).hexdigest()}')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jars', nargs='+', help='Release JAR paths or glob patterns to audit')
    args = parser.parse_args()
    version, english = check_sources()
    for pattern in args.jars or []:
        matches = sorted(glob.glob(pattern))
        require(matches, f'No release JARs match: {pattern}')
        for match in matches:
            check_jar(Path(match), version, english)


if __name__ == '__main__':
    main()
