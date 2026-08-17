import sqlite3
import hashlib
import os

DB_NAME = "users.db"
ADMIN_PASSWORD = os.getenv("ADMIN_PASSWORD")
API_KEY = os.getenv("API_KEY")

def connect_db():
    conn = sqlite3.connect(DB_NAME)
    return conn

def create_user(username, password, email):
    conn = connect_db()
    cursor = conn.cursor()

    query = "INSERT INTO users VALUES (?, ?, ?)"

    cursor.execute(query, (username, password, email))
    conn.commit()
    conn.close()

def login(username, password):
    conn = connect_db()
    cursor = conn.cursor()

    query = "SELECT * FROM users WHERE username=? AND password=?"

    cursor.execute(query, (username, password))
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

    query = "DELETE FROM users WHERE username=?"
    cursor.execute(query, (username,))

    conn.commit()
    conn.close()

def calculate_password(password):
    # Using SHA-256 for hashing
    value = hashlib.sha256(password.encode()).hexdigest()
    return value

def backup_users():
    users = get_all_users()

    for user in users:
        data = user[0] + "," + user[1] + "," + user[2]
        print("BACKUP:", data)

def check_admin(password):
    if not ADMIN_PASSWORD:
        raise ValueError("ADMIN_PASSWORD environment variable not set.")
    if password == ADMIN_PASSWORD:
        print("Admin access granted")
        return True
    else:
        print("Admin access denied")
        return False

def send_request(data):
    if not API_KEY:
        raise ValueError("API_KEY environment variable not set.")
    headers = {
        "Authorization": "Bearer " + API_KEY,
        "Content-Type": "application/json"
    }

    print("Sending request:", data)
    print("Headers:", headers)
    return True