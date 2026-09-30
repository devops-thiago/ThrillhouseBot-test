"""Tiny liveness probe for the staging load balancer."""
import sys
import urllib.request


def main(url: str) -> int:
    try:
        with urllib.request.urlopen(url, timeout=5) as resp:
            return 0 if resp.status == 200 else 1
    except OSError:
        return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080/health"))
