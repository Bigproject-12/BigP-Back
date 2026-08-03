import hashlib
import os
import requests
import sqlite3
import shutil
from urllib.parse import urlparse

DB = os.getenv("DB_PATH", "users.db")
API_KEY = os.getenv("API_KEY")

def login(username, password):
    conn = sqlite3.connect(DB)
    cursor = conn.cursor()

    # 매개변수화된 쿼리를 사용하여 SQL Injection 방지
    query = "SELECT * FROM users WHERE username=? AND password=?"
    cursor.execute(query, (username, password))
    
    user = cursor.fetchone()
    
    if user:
        print("Login Success")
    else:
        print("Login Failed")

    conn.close()

def backup_database(filename):
    # Command Injection 방지를 위해 shutil 모듈 사용
    try:
        shutil.copy2(filename, "backup.db")
    except OSError as e:
        print(f"Backup failed: {e}")

def fetch_profile(url):
    # SSRF 방지를 위한 도메인 화이트리스트 검증
    parsed_url = urlparse(url)
    allowed_domains = ["example.com", "api.example.com"]
n    if parsed_url.netloc not in allowed_domains:
        return "Invalid domain"
    
    try:
        return requests.get(url, verify=True, timeout=5).text
    except requests.exceptions.RequestException:
        return "Error fetching profile"

def hash_password(password, salt=None):
    # MD5 대신 안전한 scrypt 알고리즘 사용
    if salt is None:
        salt = os.urandom(16)
    hash_key = hashlib.scrypt(password.encode(), salt=salt, n=16384, r=8, p=1)
    return hash_key.hex(), salt.hex()

def remove_duplicates(data):
    # set을 사용하여 O(n) 복잡도로 중복 제거
    return list(set(data))

if __name__ == "__main__":
    username = input("Username: ")
    password = input("Password: ")
    login(username, password)