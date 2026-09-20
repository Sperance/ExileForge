"""Run the real Kotlin client against a packaged backend and an isolated CI database.

The backend pins its Mongo connection in code (``mongodb://localhost:27017``, database
``mongobase``) and seeds its own accounts, so the job supplies a throwaway replica set rather
than credentials. Its ``application.yaml`` names a module that does not exist, so the installed
distribution is started through the real ``main()`` instead of the generated launcher.
"""
import json
import os
import subprocess
import time
import urllib.error
import urllib.request
from pathlib import Path

root = Path(__file__).resolve().parents[1]
backend = root / 'backend'
# Credentials of the accounts the backend seeds on an empty database.
admin_password = 'P32543254'
player_login, player_password = 'test1', 'P123456'


def request(path):
    with urllib.request.urlopen('http://localhost:8080' + path, timeout=10) as response:
        return json.load(response)['data']


(root / 'build').mkdir(exist_ok=True)
classpath = str(backend / 'build/install/ktor-bestgame/lib/*')
env = dict(os.environ, JAVA_HOME=os.environ['JAVA_HOME_21_X64'])
java = str(Path(env['JAVA_HOME']) / 'bin/java')
with (root / 'build/client-server.log').open('w') as log:
    process = subprocess.Popen([java, '-cp', classpath, 'ApplicationKt'], cwd=backend, env=env, stdout=log, stderr=subprocess.STDOUT)
    try:
        deadline = time.monotonic() + 180
        while True:
            if process.poll() is not None:
                raise RuntimeError('Backend exited; inspect client-server.log')
            try:
                # Ktor prints the selector, so the method arrives as "(GET)".
                routes = {''.join(c for c in route['method'] if c.isalpha()) + ' ' + route['path'] for route in request('/system/routes')}
                assert 'GET /api/v1/character/inventory/stats' in routes, sorted(routes)
                assert 'POST /api/v1/characterequipment/applyOrb' in routes, sorted(routes)
                assert 'GET /api/v1/characterclass' in routes, sorted(routes)
                assert 'POST /api/v1/characterskillnode/allocate' in routes, sorted(routes)
                break
            except (urllib.error.URLError, TimeoutError, KeyError):
                if time.monotonic() > deadline:
                    raise
                time.sleep(.5)
        test_env = dict(os.environ, EF_LIVE_URL='http://localhost:8080/', EF_ADMIN_PASSWORD=admin_password,
                        EF_PLAYER_LOGIN=player_login, EF_PLAYER_PASSWORD=player_password)
        subprocess.run(['bash', 'gradlew', ':core:test', '--tests', 'com.sperance.exileforge.core.ServerIntegrationTest', '--rerun-tasks'],
                       cwd=root, env=test_env, check=True)
    finally:
        process.terminate()
        try:
            process.wait(timeout=20)
        except subprocess.TimeoutExpired:
            process.kill()
            process.wait()
