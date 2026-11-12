"""FRIDAY Ultra package entry point — `python -m friday_ultra` launches the CLI."""
from __future__ import annotations

import sys

from friday_ultra.main import main

if __name__ == "__main__":
    sys.exit(main())
