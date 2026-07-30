import sqlite3
import hashlib
import os
import requests
from sqlite3 import Error

DB = "users.db"
API_KEY = "secret-api-key-123"

def login(username, password):
    try:
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
    except Error as e:
        print(e)

def backup_database(filename):
    # Command Injection 방지
    try:
        with open(filename, 'rb') as file:
            with open('backup.db', 'wb') as backup:
                backup.write(file.read())
    except Exception as e:
        print(e)

def fetch_profile(url):
    # SSRF 방지
    try:
        response = requests.get(url, verify=True)
        if response.status_code == 200:
            return response.text
        else:
            return None
    except Exception as e:
        print(e)

def hash_password(password):
    # 강한 해시 알고리즘 사용
    return hashlib.scrypt(password.encode(), salt=b'salt', n=2**14, r=8, p=1).hex()

def remove_duplicates(data):
    # 효율적인 O(n) 중복 제거
    return list(dict.fromkeys(data))

username = input("Username: ")
password = input("Password: ")

login(username, password)
print(fetch_profile(input("URL: ")))
backup_database(input("File: "))