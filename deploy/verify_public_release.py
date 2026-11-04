"""Read-only HTTPS validation of the public files in a release manifest."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from urllib.parse import quote
from urllib.request import Request, urlopen
import argparse
import hashlib
import json
import time


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--manifest', required=True, type=Path)
    parser.add_argument('--origin', default='https://t1.wedodiary.top')
    parser.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    manifest = json.loads(args.manifest.read_text(encoding='utf-8'))
    artifacts = [(name, digest) for name, digest in manifest['sha256'].items()
                 if name.startswith(('admin/', 'h5/'))]
    if not artifacts:
        raise SystemExit('No public files in the manifest')

    def verify(item):
        name, digest = item
        url_path = name[len('admin/'):] if name.startswith('admin/') else 'app/' + name[len('h5/'):]
        url = args.origin.rstrip('/') + '/' + quote(url_path, safe='/')
        error = ''
        for attempt in range(3):
            try:
                request = Request(url, headers={'Accept-Encoding': 'identity', 'Cache-Control': 'no-cache'})
                with urlopen(request, timeout=30) as response:
                    body = response.read()
                    if response.status != 200 or hashlib.sha256(body).hexdigest() != digest:
                        raise ValueError('HTTP status or content hash mismatch')
                return {'file': name, 'passed': True}
            except Exception as exception:
                error = str(exception)
                if attempt < 2:
                    time.sleep(1)
        return {'file': name, 'passed': False, 'error': error}

    with ThreadPoolExecutor(max_workers=4) as pool:
        results = list(pool.map(verify, artifacts))
    failures = [result for result in results if not result['passed']]
    receipt = {'release': manifest['release'], 'origin': args.origin,
               'checkedFiles': len(results), 'passed': not failures, 'failures': failures,
               'completedAt': time.strftime('%Y-%m-%dT%H:%M:%S%z')}
    args.output.write_text(json.dumps(receipt, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(receipt, ensure_ascii=False))
    if failures:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
