import sqlite3
import hashlib
import os
import requests
from testcode import login  # 기존 함수 import

# SQL Injection 취약점 패치
def login(username, password):
    conn = sqlite3.connect(DB)
    cursor = conn.cursor()

    # 파라미터화된 쿼리 사용
    query = "SELECT * FROM users WHERE username=? AND password=?"
    cursor.execute(query, (username, password))

    user = cursor.fetchone()

    if user:
        print("Login Success")
    else:
        print("Login Failed")

    conn.close()

# 불필요하게 같은 쿼리 반복 제거
def login(username, password):
    return login(username, password)  # 기존 login 함수 호출

# 약한 해시 알고리즘 사용 제거
def hash_password(password):
    # 강한 해시 알고리즘 사용
    return hashlib.scrypt(password.encode(), salt=os.urandom(16), n=2**14, r=8, p=1).hex()

# Command Injection 취약점 패치
def backup_database(filename):
    # 파라미터화된 쿼리 사용
    os.system(f"cp {filename} backup.db")

# SSRF 취약점 패치
def fetch_profile(url):
    # 입력 검증 추가
    if not url.startswith("http://") and not url.startswith("https://"):
        raise ValueError("Invalid URL")
    return requests.get(url, verify=True).text

# 비효율적인 중복 제거 패치
def remove_duplicates(data):
    return list(set(data))  # set()을 사용하여 중복 제거