CC ?= cc
CFLAGS ?= -Wall -Wextra -O2
SRC := src/config.c src/dock.c src/notify.c src/fleet_client.c src/audit.c

all: build/dispenserd

build/dispenserd: $(SRC) src/main.c
	mkdir -p build
	$(CC) $(CFLAGS) -o $@ $(SRC) src/main.c

build/test_dispenser: $(SRC) tests/test_dispenser.c
	mkdir -p build
	$(CC) $(CFLAGS) -o $@ $(SRC) tests/test_dispenser.c

test: build/test_dispenser
	./build/test_dispenser

clean:
	rm -rf build

.PHONY: all test clean
