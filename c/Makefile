CC ?= cc
CFLAGS ?= -std=c11 -Wall -Wextra -O2
CPPFLAGS += -D_POSIX_C_SOURCE=200809L
LDLIBS_CURL ?= -lcurl

CORE_SRC = src/config.c src/reading.c src/registry.c src/validator.c src/meter_api.c src/archive.c
APP_SRC = $(CORE_SRC) src/main.c

.PHONY: all test clean

all: build/meterval

build:
	mkdir -p build

build/meterval: $(APP_SRC) | build
	$(CC) $(CPPFLAGS) $(CFLAGS) -o $@ $(APP_SRC) $(LDLIBS_CURL)

build/test_validator: $(CORE_SRC) tests/test_validator.c | build
	$(CC) $(CPPFLAGS) $(CFLAGS) -o $@ $(CORE_SRC) tests/test_validator.c

test: build/test_validator
	./build/test_validator

clean:
	rm -rf build
