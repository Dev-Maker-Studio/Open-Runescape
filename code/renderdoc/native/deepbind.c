#define _GNU_SOURCE
#include <dlfcn.h>
#include <string.h>

void *dlopen(const char *file, int mode) {
    static void *(*real)(const char *, int);
    if (!real) real = dlsym(RTLD_NEXT, "dlopen");
    return real(file, file && strstr(file, "jaggl") ? mode | RTLD_DEEPBIND : mode);
}
