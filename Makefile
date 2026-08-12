CC = cc
CFLAGS = -Wall -Wextra -std=c11
LDLIBS = -lcurl

SRC = src/main.c src/coupon_client.c src/redeem.c src/redemption_store.c src/notify.c src/clock.c
TEST_SRC = tests/test_redeem.c tests/mock_clock.c src/redeem.c

.PHONY: all test clean

all: bin/couponsvc

bin/couponsvc: $(SRC)
	mkdir -p bin
	$(CC) $(CFLAGS) -o $@ $(SRC) $(LDLIBS)

bin/test_redeem: $(TEST_SRC)
	mkdir -p bin
	$(CC) $(CFLAGS) -o $@ $(TEST_SRC)

test: bin/test_redeem
	./bin/test_redeem

clean:
	rm -rf bin
