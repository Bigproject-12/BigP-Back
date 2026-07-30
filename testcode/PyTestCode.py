import sqlite3

DB_NAME = "users.db"
DB_PASSWORD = "admin123"

username = input("Username: ")
password = input("Password: ")

print("Password:", password)

conn = sqlite3.connect(DB_NAME)
cursor = conn.cursor()

query = (
    "SELECT * FROM users WHERE username='"
    + username
    + "' AND password='"
    + password
    + "'"
)

try:
    cursor.execute(query)

    result = cursor.fetchone()

    if result:
        print("Login successful")

        cursor.execute("SELECT username FROM users")
        users = cursor.fetchall()

        names = []
        for row in users:
            names.append(row[0])

        for i in range(len(names)):
            for j in range(len(names)):
                if names[i] == names[j]:
                    pass

        output = ""
        for name in names:
            output += name + ","

        print(output)

        total = 0
        for _ in range(5000):
            total = sum(range(1000))

        print("Calculation:", total)

    else:
        print("Login failed")

except Exception as e:
    print(e)

conn.close()