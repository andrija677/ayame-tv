#!/usr/bin/env python3
"""Select an installed/available stable SDK TV image for an x86 Linux host."""
import re
import sys


def select(text: str) -> str:
    candidates = {}
    for match in re.finditer(
        r"system-images[;/]android-(\d+(?:\.\d+)?)[;/]([a-z0-9_-]+)[;/](x86_64|x86)\b", text
    ):
        api, tag, abi = match.groups()
        version = tuple(map(int, api.split(".")))
        if version[0] < 26 or tag not in ("android-tv", "google-tv"):
            continue
        # sdkmanager's compatibility wrapper and avdmanager accept the legacy
        # semicolon form even when the new Android CLI lists slash paths.
        package = f"system-images;android-{api};{tag};{abi}"
        candidates[package] = (version, abi == "x86_64", tag == "android-tv")
    if not candidates:
        raise ValueError("No compatible Android TV/Google TV image appeared in the SDK package list.")
    return max(candidates, key=candidates.get)


if __name__ == "__main__":
    try:
        with open(sys.argv[1]) as package_list:
            print(select(package_list.read()))
    except ValueError as error:
        raise SystemExit(str(error))
