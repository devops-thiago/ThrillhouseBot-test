CC ?= cc
CFLAGS ?= -std=c11 -D_POSIX_C_SOURCE=200809L -O2 -Wall -Wextra -Iinclude

SRC = src/config.c src/records.c src/archive.c src/units.c src/rollup.c src/detail.c src/report.c src/main.c
OBJ = $(SRC:.c=.o)

all: bin/caldrift

bin/caldrift: $(OBJ)
	mkdir -p bin
	$(CC) $(CFLAGS) -o $@ $(OBJ)

bin/test_drift: tests/test_drift.c src/rollup.c src/records.c
	mkdir -p bin
	$(CC) $(CFLAGS) -o $@ tests/test_drift.c src/rollup.c src/records.c

test: bin/test_drift
	./bin/test_drift

clean:
	rm -rf bin $(OBJ)

.PHONY: all test clean
