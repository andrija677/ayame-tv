#!/usr/bin/env python3
"""Create the canonical APK and update.json assets for a signed GitHub release."""
import argparse
import hashlib
import json
import re
import shutil
from pathlib import Path


def version(project: Path) -> tuple[int, str]:
    values = dict(line.split("=", 1) for line in (project / "version.properties").read_text().splitlines()
                  if line.strip() and not line.startswith("#"))
    code, name = int(values["versionCode"]), values["versionName"]
    if not 0 < code <= 2147483647 or not re.fullmatch(r"\d+\.\d+\.\d+(?:-[a-zA-Z0-9.-]+)?", name):
        raise ValueError("Invalid version.properties")
    return code, name


def create(apk: Path, output: Path, project: Path) -> dict:
    code, name = version(project)
    output.mkdir(parents=True, exist_ok=True)
    destination = output / f"ayame-tv-{name}.apk"
    if apk.resolve() != destination.resolve():
        shutil.copyfile(apk, destination)
    with destination.open("rb") as uploaded_apk:
        digest = hashlib.file_digest(uploaded_apk, "sha256").hexdigest()
    metadata = {"schemaVersion": 1, "packageName": "dev.andrija.ayametv", "versionCode": code,
                "versionName": name, "apkName": destination.name, "sha256": digest}
    (output / "update.json").write_text(json.dumps(metadata, indent=2) + "\n")
    return metadata


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    create(args.apk, args.output, Path(__file__).resolve().parent.parent)
