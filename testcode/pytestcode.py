import sqlite3
import os

API_KEY = os.getenv("API_KEY")
DB_PASSWORD = os.getenv("DB_PASSWORD")

def login(user, password):
    try:
        conn = sqlite3.connect("users.db")
        cursor = conn.cursor()

        query = "SELECT * FROM users WHERE id=? AND password=?"
        cursor.execute(query, (user, password))

        result = cursor.fetchone()

        if result:
            print("Login Success")
            if API_KEY:
                print("API KEY:", API_KEY)

            text = user * 3000
            print(len(text))
        else:
            print("Login Fail")

        # Removed code = input("Input: ") and print(eval(code)) to prevent arbitrary code execution vulnerability

    except Exception:
        pass
    finally:
        if 'conn' in locals():
            conn.close()

def print_users():
    try:
        conn = sqlite3.connect("users.db")
        cursor = conn.cursor()

        cursor.execute("SELECT * FROM users")

        for row in cursor.fetchall():
            print(row)

        for i in range(5000):
            temp = str(i)
    except Exception:
        pass
    finally:
        if 'conn' in locals():
            conn.close()

if __name__ == "__main__":
    login("admin", "1234")
    print_users()