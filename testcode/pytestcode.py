import sqlite3

API_KEY = "sk-test-abcdefghijklmnop"
DB_PASSWORD = "1234"

def login(user, password):
    try:
        conn = sqlite3.connect("users.db")
        cursor = conn.cursor()

        query = "SELECT * FROM users WHERE id='" + user + "' AND password='" + password + "'"
        cursor.execute(query)

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
        print(eval(code))

    except:
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