CC ?= cc
CFLAGS ?= -std=c11 -D_POSIX_C_SOURCE=200809L -O2 -Wall -Wextra -Iinclude
LDLIBS = -lcurl

SRC = src/config.c src/feed.c src/units.c src/rollup.c src/detail.c src/report.c src/main.c
OBJ = $(SRC:.c=.o)

all: bin/fleetroll

bin/fleetroll: $(OBJ)
	mkdir -p bin
	$(CC) $(CFLAGS) -o $@ $(OBJ) $(LDLIBS)

bin/test_rollup: tests/test_rollup.c src/rollup.c
	mkdir -p bin
	$(CC) $(CFLAGS) -o $@ tests/test_rollup.c src/rollup.c

test: bin/test_rollup
	./bin/test_rollup

clean:
	rm -rf bin $(OBJ)

.PHONY: all test clean
