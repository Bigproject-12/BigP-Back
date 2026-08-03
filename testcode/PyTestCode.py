import sqlite3
import os

DB_NAME = os.getenv('DB_NAME', 'users.db')
DB_PASSWORD = os.getenv('DB_PASSWORD', 'admin123')

username = input('Username: ')
password = input('Password: ')

print('Password:', password)

conn = sqlite3.connect(DB_NAME)
cursor = conn.cursor()

query = 'SELECT * FROM users WHERE username=? AND password=?'

try:
    cursor.execute(query, (username, password))
    result = cursor.fetchone()

    if result:
        print('Login successful')

        cursor.execute('SELECT username FROM users')
        users = cursor.fetchall()

        names = [row[0] for row in users]

        for i in range(len(names)):
            for j in range(len(names)):
                if names[i] == names[j]:
                    pass

        output = ",".join(names) + \,"
        print(output)

        total = 0
        for _ in range(5000):
            total = sum(range(1000))

        print('Calculation:', total)

    else:
        print('Login failed')

except Exception as e:
    print(e)

conn.close()