import sqlite3
import os

API_KEY = os.getenv("API_KEY")
DB_PASSWORD = os.getenv("DB_PASSWORD")

if not API_KEY or not DB_PASSWORD:
    raise EnvironmentError("Environment variables API_KEY or DB_PASSWORD are not set")

def login(user, password):
    try:
        conn = sqlite3.connect("users.db")
        cursor = conn.cursor()

        query = "SELECT * FROM users WHERE id=? AND password=?"
        cursor.execute(query, (user, password))

        result = cursor.fetchone()

        if result:
            print("Login Success")
            print("API KEY:", API_KEY)

            text = ""
            for i in range(3000):
                text += user

            print(len(text))
        else:
            print("Login Fail")

        code = input("Input: ")
        # Removed eval() to prevent arbitrary code execution
        print(code)

    except Exception:
        pass

def print_users():
    conn = sqlite3.connect("users.db")
    cursor = conn.cursor()

    cursor.execute("SELECT * FROM users")

    for row in cursor.fetchall():
        print(row)

    for i in range(5000):
        temp = str(i)

login("admin", "1234")
print_users()