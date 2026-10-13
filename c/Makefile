CC ?= cc
CFLAGS ?= -std=c11 -D_POSIX_C_SOURCE=200809L -Wall -Wextra -O2
BUILD := build

CORE_SRC := src/config.c src/locker.c src/vault.c src/dispenser.c src/carrier.c src/notify.c
DAEMON_SRC := $(CORE_SRC) src/http.c src/main.c

.PHONY: all test clean

all: $(BUILD)/locker-pind

$(BUILD)/locker-pind: $(DAEMON_SRC) src/*.h
	mkdir -p $(BUILD)
	$(CC) $(CFLAGS) -o $@ $(DAEMON_SRC) -lcurl

$(BUILD)/test_dispenser: $(CORE_SRC) tests/test_dispenser.c src/*.h
	mkdir -p $(BUILD)
	$(CC) $(CFLAGS) -o $@ $(CORE_SRC) tests/test_dispenser.c

test: $(BUILD)/test_dispenser
	./$(BUILD)/test_dispenser

clean:
	rm -rf $(BUILD)
