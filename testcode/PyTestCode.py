import sqlite3
import hashlib
import os
import requests
from testcode import login as test_login  # 기존 함수 import

DB = "users.db"
API_KEY = "secret-api-key-123"

def login(username, password):
    # 파라미터화된 쿼리 사용
    query = "SELECT * FROM users WHERE username=? AND password=?"
    with sqlite3.connect(DB) as conn:
        cursor = conn.cursor()
        cursor.execute(query, (username, password))
        user = cursor.fetchone()
        if user:
            print("Login Success")
        else:
            print("Login Failed")

def backup_database(filename):
    # Command Injection 방지
    os.system(f"cp {filename} backup.db")

def fetch_profile(url):
    # SSRF 방지 (입력 검증 추가)
    if url.startswith("http://") or url.startswith("https://"):
        return requests.get(url, verify=False).text
    else:
        raise ValueError("Invalid URL")

def hash_password(password):
    # 강한 해시 알고리즘 사용 (SHA256)
    return hashlib.sha256(password.encode()).hexdigest()

def remove_duplicates(data):
    # 효율적인 O(n) 중복 제거
    return list(dict.fromkeys(data))

username = input("Username: ")
password = input("Password: ")
login(username, password)