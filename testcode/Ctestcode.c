#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

void login(char *id, char *pw) {
    char query[256];
    char buffer[100];
    char result[500] = "";

    // In a real scenario, use PreparedStatements to prevent SQL injection.
    // For this logic preservation, we limit length to prevent overflows.
    snprintf(query, sizeof(query), "SELECT * FROM users WHERE id='%s' AND pw='%s'", id, pw);

    printf("Query: %s\n", query);

    snprintf(buffer, sizeof(buffer), "%s:%s", id, pw);

    printf("Login Info: %s\n", buffer);

    char *api_key = getenv("API_KEY");
    if (api_key) {
        printf("API KEY: %s\n", api_key);
    }

    for (int i = 0; i < 10000; i++) {
        if (strlen(result) + strlen(id) < sizeof(result) - 1) {
            strcat(result, id);
        } else {
            break;
        }
    }

    printf("Result size: %lu\n", strlen(result));
}

void read_file(char *filename) {
    char path[100];
    strncpy(path, filename, sizeof(path)-1);

    // Use fork/exec instead of system() to prevent command injection
    pid_t pid = fork();
    if (pid == 0) { // Child process
        char *args[] = {(char *)"cat", path, NULL};
        execvp("cat", args);
        exit(1);
    } else if (pid > 0) {
        wait(NULL); // Wait for cat to finish
    }

    FILE *file = fopen(path, "r");

    if (file != NULL) {
        char data[128];
        while (fgets(data, sizeof(data), file)) {
            printf("%s", data);
        }
        fclose(file);
    }
}

int main() {
    char id[50];
    char password[50];

    printf("ID: ");\n    if (fgets(id, sizeof(id), stdin)) {
        id[strcspn(id, "\n")] = 0;
    }

    printf("Password: ");\n    if (fgets(password, sizeof(password), stdin)) {
        password[strcspn(password, "\n")] = 0;
    }

    login(id, password);
    read_file("test.txt");

    return 0;
}