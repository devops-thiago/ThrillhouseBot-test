#pragma once

/* Hashes a PIN for storage. Writes exactly 64 lowercase hex characters and a
 * NUL terminator into out, and returns 0. The output never contains the
 * plaintext PIN. */
int vault_hash_pin(const char *pin, char out[65]);
