#!/usr/bin/env python3
"""Enable the Travel Guide companion services over ADB.

Thin, policy-compliant wrapper around ``tools/adb_control.py``. Only the
Travel Guide accessibility service and notification listener are touched; other
apps' entries are preserved and never printed. See ``AGENTS.md``.
"""

from __future__ import annotations

import sys

import adb_control as adb


def main() -> int:
    try:
        adb.ADB = adb.find_adb()
        serial = adb.resolve_serial(None)
    except adb.AdbError as error:
        print(f"error: {error}", file=sys.stderr)
        return 1

    print(f"Configuring companions on {serial} for guide.app only ...")
    return adb.cmd_companion(serial)


if __name__ == "__main__":
    raise SystemExit(main())
