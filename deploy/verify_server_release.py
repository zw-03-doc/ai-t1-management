"""Read-only post-deployment checks. Credentials and session tokens are never printed."""
from pathlib import Path
import argparse, hashlib, json, subprocess, urllib.request, urllib.error

BASE=Path('/opt/t1-management')

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--release',required=True)
    args=parser.parse_args()
    current=(BASE/'current').resolve(strict=True)
    if current.parent!=BASE/'releases' or current.name!=args.release:
        raise SystemExit('Current release mismatch')
    manifest=json.loads((current/'manifest.json').read_text())
    inspection=json.loads(subprocess.check_output(['docker','inspect','t1-management-api-1','t1-management-web-1']))
    api=next(x for x in inspection if x['Name']=='/t1-management-api-1')
    assert all(x['State']['Running'] for x in inspection)
    mounts={x['Destination']:x['Source'] for x in api['Mounts']}
    assert Path(mounts['/app/app.jar'])==current/'app.jar'
    assert hashlib.sha256((current/'app.jar').read_bytes()).hexdigest()==manifest['sha256']['app.jar']
    # Check the mounted production profile and both loaded T1 proxy configurations.
    for container, config, lines in [
        ('t1-management-api-1','/app/config/application-production.yml',('max-file-size: 50MB','max-request-size: 55MB')),
        ('t1-management-web-1','/etc/nginx/conf.d/default.conf',('client_max_body_size 55m;',)),
        ('sunmoonlake-ops-web-1','/etc/nginx/tls/t1/gateway.conf',('client_max_body_size 55m;',)),
    ]:
        live=subprocess.check_output(['docker','exec',container,'cat',config]).decode()
        assert all(line in {s.strip() for s in live.splitlines()} for line in lines), 'Upload limit not applied'
    env=dict(item.split('=',1) for item in api['Config']['Env'] if '=' in item)
    assert env.get('WECHAT_MINI_APPID')=='wx556bef49cde7a986'
    assert env.get('WECHAT_MINI_SECRET')==Path('/etc/t1-management/secrets/wechat_mini_secret').read_text().strip()
    with urllib.request.urlopen('https://t1.wedodiary.top/prod-api/auth/code',timeout=30) as response:
        assert response.status==200 and json.load(response)['code']==200
    with urllib.request.urlopen('https://t1.wedodiary.top/prod-api/fitness/mini/portal/venue',timeout=30) as response:
        venue=json.load(response)
        assert venue.get('code')==200 and isinstance(venue['data']['photos'],list)
    request=urllib.request.Request('https://t1.wedodiary.top/prod-api/fitness/member/list',headers={'clientid':'e5cd7e4891bf95d1d19206ce24a7b32e'})
    try:
        response=urllib.request.urlopen(request,timeout=30)
    except urllib.error.HTTPError as exception:
        response=exception
    with response:
        status=response.status
        body=json.load(response)
        assert status in (200,401,403) and body.get('code') in (401,403)
    statement="""SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='t1_fitness'
        AND ((table_name='fit_order' AND column_name='bind_coach_id')
          OR (table_name='fit_training_set' AND column_name='target_set_index')
          OR (table_name='fit_member' AND column_name IN ('contact_mobile','profile_setup_status')));
        SELECT COUNT(*) FROM fit_store WHERE store_code LIKE 'QA_FLOW_1104_%';
        SELECT COUNT(*) FROM sys_menu c JOIN sys_menu p ON p.menu_id=c.parent_id
          WHERE c.component='fitness/venue/index' AND c.menu_name='场馆图片' AND c.visible='0' AND c.status='0'
            AND p.menu_type='M' AND p.visible='0' AND p.status='0';
        SELECT COUNT(*) FROM sys_menu c LEFT JOIN sys_menu p ON c.parent_id=p.menu_id
          WHERE c.component LIKE 'fitness/%' AND c.menu_type='C' AND p.menu_id IS NULL;
        SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='t1_fitness'
          AND table_name IN ('fit_policy_consent','fit_venue_gallery','fit_venue_photo');"""
    result=subprocess.run(['docker','exec','-i','t1-management-mysql-1','sh','-c',
        'MYSQL_PWD="$(cat /run/secrets/database_root_password)" exec mysql -uroot --default-character-set=utf8mb4 -N -B t1_fitness'],
        input=statement.encode(),stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True).stdout.decode().split()
    assert result==['4','0','1','0','3'], 'Schema, visible menu hierarchy, or isolated-fixture boundary mismatch'
    receipt={'passed':True,'release':args.release,'runningArtifactsMatch':True,'wechatConfigured':True,
             'authEndpointHealthy':True,'anonymousMemberAccessDenied':True,
             'incrementalColumnsPresent':True,'candidateStoreFixturesAbsent':True,'publicGalleryAndAdminMenuPresent':True,'uploadLimitsApplied':True,
             'scope':'Read-only production smoke; authenticated member/coach/admin business flows were tested in the isolated candidate.'}
    (current/'production-verification.json').write_text(json.dumps(receipt,indent=2))
    print(json.dumps(receipt))

if __name__=='__main__':main()
