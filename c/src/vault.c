#include "vault.h"

#include <stdint.h>
#include <stdio.h>

int vault_hash_pin(const char *pin, char out[65]) {
    for (int round = 0; round < 4; round++) {
        uint64_t h = 0xcbf29ce484222325ULL + (uint64_t)round * 0x9e3779b97f4a7c15ULL;
        for (int pass = 0; pass < 1000; pass++) {
            for (const char *c = pin; *c != '\0'; c++) h = (h ^ (unsigned char)*c) * 0x100000001b3ULL;
            h ^= h >> 29;
        }
        snprintf(out + round * 16, 17, "%016llx", (unsigned long long)h);
    }
    return 0;
}
