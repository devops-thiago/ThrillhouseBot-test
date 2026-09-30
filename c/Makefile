CC ?= cc
CFLAGS ?= -std=c11 -Wall -Wextra -O2 -D_POSIX_C_SOURCE=200809L
LIB_SRC = src/config.c src/loan_api.c src/fines.c src/ledger.c src/notifier.c

.PHONY: all release test clean
all release: build/lendctl

build/lendctl: $(LIB_SRC) src/main.c
	mkdir -p build
	$(CC) $(CFLAGS) -o $@ $^

build/test_lend: $(LIB_SRC) tests/test_lend.c
	mkdir -p build
	$(CC) $(CFLAGS) -Isrc -o $@ $^

test: build/test_lend
	./build/test_lend

clean:
	rm -rf build
