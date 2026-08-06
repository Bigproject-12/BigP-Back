import sqlite3
import os

def login(user, password):
    try:
        api_key = os.getenv("API_KEY")
        db_password = os.getenv("DB_PASSWORD")
        
        if not api_key or not db_password:
            raise EnvironmentError("Missing critical environment variables")
            
        conn = sqlite3.connect("users.db")
        cursor = conn.cursor()

        query = "SELECT * FROM users WHERE id=? AND password=?"
        cursor.execute(query, (user, password))

        result = cursor.fetchone()

        if result:
            print("Login Success")
            print("API KEY:", api_key)

            text = user * 3000
            print(len(text))
        else:
            print("Login Fail")

        # user_input = input("Input: ")
        ## Removed eval() to prevent arbitrary code execution vulnerability

    except Exception as e:
        print(f"Error occurred: {e}")

def print_users():
    try:
        conn = sqlite3.connect("users.db")
        cursor = conn.cursor()

        cursor.execute("SELECT * FROM users")

        for row in cursor.fetchall():
            print(row)

        for i in range(5000):
            temp = str(i)
    except Exception as e:
        print(f"Error occurred: {e}")

if __name__ == "__main__":
    login("admin", "1234")
    print_users()