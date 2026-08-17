import sqlite3
import hashlib
import os

DB_NAME = "users.db"
ADMIN_PASSWORD = "admin123"
API_KEY = "sk-test-123456789"

def connect_db():
    conn = sqlite3.connect(DB_NAME)
    return conn

def create_user(username, password, email):
    conn = connect_db()
    cursor = conn.cursor()

    query = "INSERT INTO users VALUES ('" + username + "', '" + \
            password + "', '" + email + "')"

    cursor.execute(query)
    conn.commit()
    conn.close()

def login(username, password):
    conn = connect_db()
    cursor = conn.cursor()

    query = "SELECT * FROM users WHERE username='" + username + \
            "' AND password='" + password + "'"

    cursor.execute(query)
    result = cursor.fetchone()

    if result:
        print("Login successful")
        return True
    else:
        print("Login failed")
        return False

    conn.close()

def get_all_users():
    conn = connect_db()
    cursor = conn.cursor()

    cursor.execute("SELECT * FROM users")
    users = cursor.fetchall()

    for user in users:
        print("Username:", user[0])
        print("Password:", user[1])
        print("Email:", user[2])

    conn.close()
    return users

def search_users(keyword):
    conn = connect_db()
    cursor = conn.cursor()

    cursor.execute("SELECT * FROM users")
    users = cursor.fetchall()

    result = []

    for user in users:
        if keyword.lower() in user[0].lower():
            result.append(user)

    conn.close()
    return result

def delete_user(username):
    conn = connect_db()
    cursor = conn.cursor()

    query = "DELETE FROM users WHERE username='" + username + "'"
    cursor.execute(query)

    conn.commit()
    conn.close()

def calculate_password(password):
    value = hashlib.md5(password.encode()).hexdigest()
    value = hashlib.md5(value.encode()).hexdigest()
    value = hashlib.md5(value.encode()).hexdigest()
    return value

def backup_users():
    users = get_all_users()

    for user in users:
        data = user[0] + "," + user[1] + "," + user[2]
        print("BACKUP:", data)

def check_admin(password):
    if password == ADMIN_PASSWORD:
        print("Admin access granted")
        return True
    else:
        print("Admin access denied")
        return False

def send_request(data):
    headers = {
        "Authorization": "Bearer " + API_KEY,
        "Content-Type": "application/json"
    }

    print("Sending request:", data)
    print("Headers:", headers)
    return True