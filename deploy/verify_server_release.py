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
    env=dict(item.split('=',1) for item in api['Config']['Env'] if '=' in item)
    assert env.get('WECHAT_MINI_APPID')=='wx556bef49cde7a986'
    assert env.get('WECHAT_MINI_SECRET')==Path('/etc/t1-management/secrets/wechat_mini_secret').read_text().strip()
    with urllib.request.urlopen('https://t1.wedodiary.top/prod-api/auth/code',timeout=30) as response:
        assert response.status==200 and json.load(response)['code']==200
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
          OR (table_name='fit_training_set' AND column_name='target_set_index'));
        SELECT COUNT(*) FROM fit_store WHERE store_code LIKE 'QA_FLOW_1104_%';"""
    result=subprocess.run(['docker','exec','-i','t1-management-mysql-1','sh','-c',
        'MYSQL_PWD="$(cat /run/secrets/database_root_password)" exec mysql -uroot -N -B t1_fitness'],
        input=statement.encode(),stdout=subprocess.PIPE,stderr=subprocess.PIPE,check=True).stdout.decode().split()
    assert result==['2','0'], 'Schema or isolated-fixture boundary mismatch'
    receipt={'passed':True,'release':args.release,'runningArtifactsMatch':True,'wechatConfigured':True,
             'authEndpointHealthy':True,'anonymousMemberAccessDenied':True,
             'incrementalColumnsPresent':True,'candidateStoreFixturesAbsent':True,
             'scope':'Read-only production smoke; authenticated member/coach/admin business flows were tested in the isolated candidate.'}
    (current/'production-verification.json').write_text(json.dumps(receipt,indent=2))
    print(json.dumps(receipt))

if __name__=='__main__':main()
