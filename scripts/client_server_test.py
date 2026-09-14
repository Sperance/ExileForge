"""Run the real Kotlin client against a packaged backend and an isolated CI database."""
import json
import os
from pathlib import Path
import secrets
import subprocess
import time
import urllib.request
import urllib.error

root = Path(__file__).resolve().parents[1]
assert os.environ.get('MONGO_DB') == 'exileforge_client_integration'
admin_password = secrets.token_urlsafe(24)
player_password = secrets.token_urlsafe(24)
player_login = 'client_' + secrets.token_hex(6)
env = dict(os.environ, SEED_DEMO_DATA='true', SEED_ADMIN_PASSWORD=admin_password,
           JAVA_HOME=os.environ['JAVA_HOME_21_X64'])
def request(path, body=None, token=None):
    headers = {'Content-Type': 'application/json'}
    if token: headers['Authorization'] = 'Bearer ' + token
    req = urllib.request.Request('http://localhost:8080' + path, data=json.dumps(body).encode() if body is not None else None, headers=headers)
    with urllib.request.urlopen(req, timeout=10) as response:
        return json.load(response)['data']
(root / 'build').mkdir(exist_ok=True)
with (root / 'build/client-server.log').open('w') as log:
    process = subprocess.Popen([str(root / 'backend/build/install/ktor-bestgame/bin/ktor-bestgame')], cwd=root / 'backend', env=env, stdout=log, stderr=subprocess.STDOUT)
    try:
        deadline = time.monotonic() + 120
        while True:
            if process.poll() is not None: raise RuntimeError('Backend exited; inspect client-server.log')
            try:
                assert request('/api/v1/poe/capabilities')['apiRevision'] >= 3
                break
            except (urllib.error.URLError, TimeoutError):
                if time.monotonic() > deadline: raise
                time.sleep(.5)
        token = request('/api/v1/poe/token', {'login': 'admin', 'password': admin_password})['token']
        request('/api/v1/user', [{'name': player_login, 'login': player_login, 'email': player_login + '@example.test', 'password': player_password, 'age': 25}], token)
        test_env = dict(os.environ, EF_LIVE_URL='http://localhost:8080/', EF_ADMIN_PASSWORD=admin_password, EF_PLAYER_LOGIN=player_login, EF_PLAYER_PASSWORD=player_password)
        subprocess.run(['bash', 'gradlew', ':core:test', '--tests', 'com.sperance.exileforge.core.ServerIntegrationTest', '--rerun-tasks'], cwd=root, env=test_env, check=True)
    finally:
        process.terminate()
        try: process.wait(timeout=20)
        except subprocess.TimeoutExpired: process.kill(); process.wait()
