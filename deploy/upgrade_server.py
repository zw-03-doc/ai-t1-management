"""Upgrade only T1 after candidate validation; preserve an application rollback target."""
from pathlib import Path
import argparse, gzip, hashlib, json, os, re, shutil, socket, subprocess, tempfile, time, urllib.request, urllib.parse

BASE = Path('/opt/t1-management')
PROTECTED = ['t1-management-mysql-1', 't1-management-redis-1', 'sunmoonlake-ops-web-1', 'sunmoonlake-ops-api-1', 'sunmoonlake-ops-postgres-1']
EXPECTED_MIGRATIONS = (
    'V1.3.0__identity_and_orders.sql',
    'V1.3.1__booking_flow.sql',
    'V1.3.2__training_lifecycle.sql',
    'V1.3.3__member_optional_profile.sql',
    'V1.3.4__portal_slogans.sql',
    'V1.3.5__fitness_navigation.sql',
)

def release_file(release, name):
    file = (release / name).resolve(strict=True)
    file.relative_to(release)
    if not file.is_file():
        raise ValueError('Expected a release file: ' + name)
    return file

def reviewed_migrations(manifest):
    found = {}
    for item in manifest['sql_sources']:
        name = Path(item['source']).name
        version = re.match(r'V(\d+)\.(\d+)\.(\d+)', name)
        if version and tuple(map(int, version.groups())) > (1, 2, 3) and name not in EXPECTED_MIGRATIONS:
            raise ValueError('Unreviewed incremental migration: ' + name)
        if name in EXPECTED_MIGRATIONS:
            if not re.fullmatch(r'\d+\.sql', item['file']):
                raise ValueError('Unexpected staged SQL filename')
            if name in found:
                raise ValueError('Duplicate incremental migration: ' + name)
            found[name] = item
    if set(found) != set(EXPECTED_MIGRATIONS):
        raise ValueError('Reviewed incremental migration set is incomplete')
    return [found[name] for name in EXPECTED_MIGRATIONS]

def read_env(path):
    return dict(line.split('=', 1) for line in path.read_text().splitlines() if '=' in line and not line.startswith('#'))

def run(command, **kwargs):
    return subprocess.run(command, check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, **kwargs)

def snapshot():
    values = json.loads(run(['docker', 'inspect', *PROTECTED]).stdout)
    return {v['Name'].lstrip('/'): {'id': v['Id'], 'started': v['State']['StartedAt'], 'restarts': v['RestartCount']} for v in values}

def application_snapshot():
    values = json.loads(run(['docker', 'inspect', 't1-management-api-1', 't1-management-web-1']).stdout)
    # Exclude Config.Env: it contains real production secrets.
    return {v['Name'].lstrip('/'): {'id': v['Id'], 'imageId': v['Image'], 'image': v['Config']['Image'],
        'mounts': [{'source': item['Source'], 'target': item['Destination'], 'rw': item['RW']} for item in v['Mounts']]} for v in values}

def mysql(statement):
    return run(['docker', 'exec', '-i', 't1-management-mysql-1', 'sh', '-c',
        'MYSQL_PWD="$(cat /run/secrets/database_root_password)" exec mysql -uroot --default-character-set=utf8mb4 t1_fitness'], input=statement.encode())

def http(url):
    with urllib.request.urlopen(url, timeout=20) as response:
        return response.status, response.read()

def wait_api():
    deadline = time.monotonic() + 360
    while time.monotonic() < deadline:
        try:
            status, body = http('https://t1.wedodiary.top/prod-api/auth/code')
            if status == 200 and json.loads(body).get('code') == 200:
                return
        except Exception:
            pass
        time.sleep(6)
    raise RuntimeError('T1 API did not become healthy within six minutes')

def switch(compose):
    run(['docker', 'compose', '-p', 't1-management', '-f', str(compose), 'up', '--no-deps', '--no-build', '--pull', 'never', '-d', 'api', 'web'])

def verify_static(manifest):
    for url, target, prefix in [('https://t1.wedodiary.top/', 'admin/index.html', 'admin/'), ('https://t1.wedodiary.top/app/', 'h5/index.html', 'h5/')]:
        status, body = http(url)
        if status != 200 or hashlib.sha256(body).hexdigest() != manifest['sha256'][target]:
            raise RuntimeError('Published homepage does not match release')
        # Catch a switched HTML page whose entry JavaScript/CSS still points to missing or old files.
        for resource in re.findall(r'(?:src|href)=["\']([^"\']+)["\']', body.decode('utf-8')):
            absolute = urllib.parse.urljoin(url, resource)
            parsed = urllib.parse.urlsplit(absolute)
            if parsed.netloc != 't1.wedodiary.top' or not parsed.path.endswith(('.js', '.css')):
                continue
            relative = parsed.path.removeprefix('/app/') if prefix == 'h5/' else parsed.path.lstrip('/')
            expected = manifest['sha256'].get(prefix + relative)
            status, content = http(absolute)
            if not expected or status != 200 or hashlib.sha256(content).hexdigest() != expected:
                raise RuntimeError('Published entry resource does not match release: ' + parsed.path)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--release', required=True)
    parser.add_argument('--wechat-secret-file', type=Path)
    args = parser.parse_args()
    if not re.fullmatch(r'[a-zA-Z0-9-]+', args.release):
        raise SystemExit('Invalid release name')
    if '7xvbo7bswbug7bn1m8gt' not in socket.gethostname():
        raise SystemExit('Unexpected host; refusing deployment')
    release = (BASE / 'releases' / args.release).resolve(strict=True)
    release.relative_to(BASE / 'releases')
    current = (BASE / 'current').resolve(strict=True)
    current.relative_to(BASE / 'releases')
    if current == release:
        raise SystemExit('Release is already current; refusing to overwrite its deployment evidence')
    manifest = json.loads((release / 'manifest.json').read_text())
    if manifest.get('release') != args.release:
        raise SystemExit('Release name does not match manifest')
    required = {'compose.yml', 'application-production.yml', 'web.conf', 'app.jar', 'admin/index.html', 'h5/index.html'}
    if not required.issubset(manifest['sha256']):
        raise SystemExit('Manifest is missing required deployment artifacts')
    for name, digest in manifest['sha256'].items():
        file = release_file(release, name)
        if hashlib.sha256(file.read_bytes()).hexdigest() != digest:
            raise SystemExit('Release checksum mismatch: ' + name)
    migrations = reviewed_migrations(manifest)
    previous_manifest = json.loads((current / 'manifest.json').read_text())
    previous_sql = {Path(item['source']).name: previous_manifest['sha256']['sql/' + item['file']]
                    for item in previous_manifest.get('sql_sources', [])}
    for migration in migrations:
        name = Path(migration['source']).name
        if name in previous_sql and previous_sql[name] != manifest['sha256']['sql/' + migration['file']]:
            raise SystemExit('Previously released migration changed: ' + name)
    migrations = [item for item in migrations if Path(item['source']).name not in previous_sql]
    verification = json.loads((release / 'candidate-verification.json').read_text())
    if verification.get('passed') is not True or verification['jarSha256'] != manifest['sha256']['app.jar']:
        raise SystemExit('Candidate validation does not match the release JAR')
    for receipt_key, artifact in [('adminIndexSha256', 'admin/index.html'), ('h5IndexSha256', 'h5/index.html')]:
        if verification.get(receipt_key) != manifest['sha256'][artifact]:
            raise SystemExit('Candidate validation does not match frontend artifact: ' + artifact)
    for item in migrations:
        name = Path(item['source']).name
        digest = manifest['sha256'].get('sql/' + item['file'])
        release_file(release, 'sql/' + item['file'])
        if not digest or verification.get('migrationSha256', {}).get(name) != digest:
            raise SystemExit('Candidate migration validation does not match: ' + name)
    env_path = Path('/etc/t1-management/production.env')
    env = read_env(env_path)
    for key in ('T1_DATABASE_PASSWORD', 'T1_REDIS_PASSWORD', 'T1_JWT_SECRET_KEY'):
        if not env.get(key):
            raise SystemExit('Required production configuration is missing: ' + key)
    if env.get('WECHAT_MINI_APPID') not in (None, '', 'wx556bef49cde7a986'):
        raise SystemExit('WeChat AppID mismatch')
    if args.wechat_secret_file:
        secret = args.wechat_secret_file.read_text().strip()
        if len(secret) != 32 or not all(c in '0123456789abcdefABCDEF' for c in secret):
            raise SystemExit('Unexpected WeChat secret format')
        env.update(WECHAT_MINI_APPID='wx556bef49cde7a986', WECHAT_MINI_SECRET=secret)
    # Parse both compose files before backups or SQL; output only service names, never resolved secrets.
    for compose in (current / 'compose.yml', release / 'compose.yml'):
        services = run(['docker', 'compose', '-p', 't1-management', '-f', str(compose), 'config', '--services']).stdout.decode().split()
        if set(services) != {'mysql', 'redis', 'api', 'web'}:
            raise SystemExit('Unexpected services in T1 compose')
    before = snapshot()
    backup = BASE / 'backups' / args.release
    backup.mkdir(mode=0o700, exist_ok=False)
    (backup / 'before.json').write_text(json.dumps({'current': str(current), 'containers': before}, indent=2))
    (backup / 'previous-applications.json').write_text(json.dumps(application_snapshot(), indent=2))
    old_receipt = BASE / 'deployment-receipt.json'
    if old_receipt.exists():
        shutil.copy2(old_receipt, backup / 'deployment-receipt.json')
    shutil.copy2(current / 'compose.yml', backup / 'previous-compose.yml')
    with gzip.open(backup / 'database.sql.gz', 'wb') as output, tempfile.TemporaryFile() as errors:
        dump = subprocess.Popen(['docker', 'exec', 't1-management-mysql-1', 'sh', '-c',
            'MYSQL_PWD="$(cat /run/secrets/database_root_password)" exec mysqldump -uroot --single-transaction --skip-lock-tables --set-gtid-purged=OFF --no-tablespaces t1_fitness'], stdout=subprocess.PIPE, stderr=errors)
        shutil.copyfileobj(dump.stdout, output)
        dump.stdout.close()
        if dump.wait() != 0:
            raise RuntimeError('Database backup failed')
    run(['tar', '-czf', str(backup / 'media.tar.gz'), '-C', '/var/lib/t1-management', 'media'])
    shutil.copy2(env_path, backup / 'production.env')
    os.chmod(backup / 'production.env', 0o600)
    applied = []
    active_migration = None
    switch_attempted = False
    try:
        for item in migrations:
            active_migration = Path(item['source']).name
            (backup / 'migration-progress.json').write_text(json.dumps({'applied': applied, 'attempting': active_migration, 'databaseKeptOnRollback': True}))
            mysql(release_file(release, 'sql/' + item['file']).read_text())
            applied.append(active_migration)
            active_migration = None
            (backup / 'migration-progress.json').write_text(json.dumps({'applied': applied, 'attempting': None, 'databaseKeptOnRollback': True}))
        print(json.dumps({'phase': 'backed_up_and_migrated', 'migrations': len(applied)}), flush=True)
        if args.wechat_secret_file:
            temp = env_path.with_suffix('.next')
            # Create privately, before writing any secret bytes.
            with open(os.open(temp, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'w') as output:
                output.write(''.join(k + '=' + v + '\n' for k, v in env.items()))
            temp.replace(env_path)
        switch_attempted = True
        switch(release / 'compose.yml')
        print(json.dumps({'phase': 'waiting_for_T1_API'}), flush=True)
        wait_api()
        verify_static(manifest)
        after = snapshot()
        if before != after:
            raise RuntimeError('Protected services changed during deployment')
    except Exception:
        rollback = {'target': str(current), 'applicationSwitchAttempted': switch_attempted,
            'databaseKept': True, 'appliedMigrations': applied, 'possiblyPartialMigration': active_migration, 'status': 'attempting'}
        (backup / 'rollback.json').write_text(json.dumps(rollback))
        try:
            shutil.copy2(backup / 'production.env', env_path)
            if switch_attempted:
                switch(current / 'compose.yml')
                wait_api()
            rollback.update(status='completed', rolledBackTo=str(current))
        except Exception as recovery_error:
            rollback.update(status='failed', recoveryError=type(recovery_error).__name__)
            raise RuntimeError('T1 application rollback failed; inspect protected backup and current service state') from recovery_error
        finally:
            (backup / 'rollback.json').write_text(json.dumps(rollback))
        raise
    link = BASE / 'current.next'
    if link.is_symlink():
        link.unlink()
    link.symlink_to(release, target_is_directory=True)
    link.replace(BASE / 'current')
    receipt = {'release': args.release, 'source_revision': manifest.get('source_revision'), 'previous': str(current), 'backup': str(backup), 'jarSha256': manifest['sha256']['app.jar'],
        'protectedContainersUnchanged': True, 'wechatAppid': env.get('WECHAT_MINI_APPID'), 'wechatConfigured': bool(env.get('WECHAT_MINI_SECRET')),
        'completedAt': time.strftime('%Y-%m-%dT%H:%M:%S%z'), 'migrations': migrations}
    (BASE / 'deployment-receipt.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2))
    (release / 'deployment-receipt.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2))
    print(json.dumps(receipt, ensure_ascii=False), flush=True)

if __name__ == '__main__':
    main()
