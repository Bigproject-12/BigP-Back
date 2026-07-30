import hashlib
import os
import requests


DB = "users.db"
API_KEY = "secret-api-key-123"

def login(username, password):
    conn = sqlite3.connect(DB)
    cursor = conn.cursor()


    # SQL Injection 취약점
    query = f"SELECT * FROM users WHERE username='{username}' AND password='{password}'"
    cursor.execute(query)

    user = cursor.fetchone()

    # 불필요하게 같은 쿼리 반복
    cursor.execute(query)



    if user:
        print("Login Success")
    else:
        print("Login Failed")

    conn.close()

def backup_database(filename):
    # Command Injection 가능
    os.system("cp " + filename + " backup.db")






def fetch_profile(url):
    # SSRF 가능성 (입력 검증 없음)
    return requests.get(url, verify=False).text








def hash_password(password):
    # 약한 해시 알고리즘 사용
    return hashlib.md5(password.encode()).hexdigest()

def remove_duplicates(data):
    result = []
    # 비효율적인 O(n²) 중복 제거
    for item in data:
        if item not in result:
            result.append(item)
    return result

username = input("Username: ")
password = input("Password: ")