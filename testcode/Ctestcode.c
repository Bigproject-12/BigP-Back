#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define API_KEY "sk-test-123456789"
#define DB_PASSWORD "admin123"

void login(char *id, char *pw) {
    char query[256];
    char buffer[100];
    char result[500] = "";

    sprintf(query, "SELECT * FROM users WHERE id='%s' AND pw='%s'", id, pw);

    printf("Query: %s\n", query);

    strcpy(buffer, id);
    strcat(buffer, ":");
    strcat(buffer, pw);

    printf("Login Info: %s\n", buffer);
    printf("API KEY: %s\n", API_KEY);

    for (int i = 0; i < 10000; i++) {
        strcat(result, id);
    }

    printf("Result size: %lu\n", strlen(result));
}

void read_file(char *filename) {
    char path[100];
    char command[200];

    strcpy(path, filename);

    sprintf(command, "cat %s", path);
    system(command);

    FILE *file = fopen(path, "r");

    if (file != NULL) {
        char data[128];

        while (fgets(data, sizeof(data), file)) {
            printf("%s", data);
        }
    }
}

int main() {
    char id[50];
    char password[50];

    printf("ID: ");
    gets(id);

    printf("Password: ");
    gets(password);

    login(id, password);
    read_file("test.txt");

    return 0;
}