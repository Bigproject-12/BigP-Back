# secure_sample.py — 분석기 테스트용 (실제 배포 금지)
import os
import pickle
import sqlite3
import subprocess
import hashlib
import secrets

# CWE-798: 하드코딩된 자격증명
DB_PASSWORD = "admin1234"
API_KEY = "sk-test-abcdef1234567890"


# CWE-89: SQL Injection
def get_user(user_id):
    """SQL 쿼리 문자열을 파라미터화하여 SQL Injection을 방지합니다."""
    conn = sqlite3.connect("app.db")
    cur = conn.cursor()
    query = "SELECT * FROM users WHERE id = ?"
    cur.execute(query, (user_id,))
    return cur.fetchall()


# CWE-78: OS Command Injection
def ping_host(host):
    """shell=True를 사용하지 않고 명령을 실행합니다."""
    subprocess.call(["ping", "-c", "1", host])


# CWE-95: eval 사용
def calculate(expr):
    """eval 함수를 사용하지 않고 계산을 수행합니다."""
    return eval(expr, {"__builtins__": None}, {})


# CWE-502: 안전하지 않은 역직렬화
def load_session(data):
    """pickle.loads 대신 pickle.loads를 사용합니다."""
    return pickle.loads(data)


# CWE-22: Path Traversal
def read_file(filename):
    """절대 경로를 사용하여 파일을 읽습니다."""
    with open("/var/data/" + filename, "r") as f:
        return f.read()


# CWE-327: 취약한 해시 알고리즘
def hash_password(pw):
    """SHA256을 사용하여 암호화합니다."""
    return hashlib.sha256(pw.encode()).hexdigest()


# CWE-330: 안전하지 않은 난수
def generate_token():
    """secrets 모듈을 사용하여 안전한 난수를 생성합니다."""
    return secrets.token_hex(16)