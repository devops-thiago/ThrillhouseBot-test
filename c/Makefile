CC ?= cc
CFLAGS ?= -std=c11 -Wall -Wextra -O2
SRC := $(filter-out src/main.c,$(wildcard src/*.c))

all: build/elevator-analyser

build/elevator-analyser: $(wildcard src/*.c) $(wildcard src/*.h)
	mkdir -p build
	$(CC) $(CFLAGS) -o $@ src/*.c

build/run-tests: $(SRC) tests/test_analyser.c $(wildcard src/*.h)
	mkdir -p build
	$(CC) $(CFLAGS) -o $@ $(SRC) tests/test_analyser.c

test: build/run-tests
	./build/run-tests

clean:
	rm -rf build

.PHONY: all test clean
