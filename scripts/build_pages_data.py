#!/usr/bin/env python3
"""Prepare static catalog pages from the unchanged measured A/E fixture.

This generates publication assets; it does not modify either measured condition.
Only Python's standard library is required. It never enables or deploys Pages.
"""

from datetime import datetime
import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
ASSET = Path("app/src/main/assets/catalog/products.json")
OUTPUT = ROOT / "docs/data/catalog-v1"


def digest(data):
    return hashlib.sha256(data).hexdigest()


def cursor(snapshot_id, limit, offset):
    identity = f"catalog-cursor-v1|{snapshot_id}|{limit}|{offset}"
    return digest(identity.encode("utf-8"))


def encode(value):
    return (json.dumps(value, ensure_ascii=False, separators=(",", ":"),
                       allow_nan=False) + "\n").encode("utf-8")


def main():
    a = (ROOT / "conditions/A" / ASSET).read_bytes()
    e = (ROOT / "conditions/E" / ASSET).read_bytes()
    if a != e:
        raise ValueError("Measured A/E fixture bytes differ")
    source = json.loads(a)
    snapshot_id = source["snapshotId"]
    items = source["items"]
    if snapshot_id != "catalog-v1" or len(items) != 500:
        raise ValueError("Expected the fixed catalog-v1, 500-product snapshot")
    if source["pageInfo"] != {"nextCursor": None, "totalCount": len(items)}:
        raise ValueError("Expected a full snapshot, not a page")
    if len({item["id"] for item in items}) != len(items):
        raise ValueError("Duplicate product IDs")
    # Two stable sorts implement Instant descending, then ID ascending.
    ordered = sorted(items, key=lambda item: item["id"])
    ordered.sort(key=lambda item: datetime.fromisoformat(
        item["updatedAt"].replace("Z", "+00:00")), reverse=True)
    files = {}

    def write(relative, data):
        output = OUTPUT / relative
        output.parent.mkdir(parents=True, exist_ok=True)
        if output.exists() and output.read_bytes() != data:
            raise ValueError(f"Existing generated file differs: {relative}")
        output.write_bytes(data)
        files[relative] = {"sha256": digest(data), "bytes": len(data)}

    # Full snapshot preserves the measured fixture's bytes and original ID order.
    write("products.json", a)
    write("empty.json", encode({"snapshotId": snapshot_id, "items": [],
                                "pageInfo": {"nextCursor": None, "totalCount": 0}}))
    limit_indexes = {}
    page_count = 0
    for limit in range(1, 101):
        directory = f"limits/{limit}"
        accepted_cursors = {}
        pages = []
        for offset in range(0, len(ordered), limit):
            input_cursor = None if offset == 0 else cursor(snapshot_id, limit, offset)
            name = "first.json" if input_cursor is None else f"{input_cursor}.json"
            end = min(offset + limit, len(ordered))
            page = {"snapshotId": snapshot_id, "items": ordered[offset:end],
                    "pageInfo": {"nextCursor": cursor(snapshot_id, limit, end)
                                 if end < len(ordered) else None,
                                 "totalCount": len(ordered)}}
            relative = f"{directory}/{name}"
            data = encode(page)
            write(relative, data)
            pages.append({"inputCursor": input_cursor, "file": name,
                          "itemCount": end - offset, "nextCursor": page["pageInfo"]["nextCursor"],
                          "sha256": digest(data), "bytes": len(data)})
            if input_cursor is not None:
                accepted_cursors[input_cursor] = name
            page_count += 1
        index = {"snapshotId": snapshot_id, "limit": limit,
                 "totalCount": len(ordered), "firstPage": "first.json",
                 "cursors": accepted_cursors, "pages": pages}
        write(f"{directory}/index.json", encode(index))
        limit_indexes[str(limit)] = f"{directory}/index.json"
    manifest = {
        "formatVersion": 1, "snapshotId": snapshot_id, "defaultLimit": 50,
        "minimumLimit": 1, "maximumLimit": 100, "totalCount": len(ordered),
        "sourceFixture": {"file": "products.json", "sha256": digest(a), "bytes": len(a)},
        "ordering": ["updatedAt descending (Instant)", "id ascending"],
        "cursorAlgorithm": "SHA-256 UTF-8 catalog-cursor-v1|snapshotId|limit|offset",
        "normal": {"limitIndexes": limit_indexes, "pageCount": page_count},
        "empty": {"file": "empty.json", "acceptsOnlyNullCursor": True},
        "errors": "Invalid requests and ERROR/NEXT_PAGE_ERROR are handled by a future client adapter; Pages does not produce the measured dynamic HTTP 400/503 responses.",
        "httpVariantMeasured": False,
        "files": files,
    }
    data = encode(manifest)
    path = OUTPUT / "manifest.json"
    if path.exists() and path.read_bytes() != data:
        raise ValueError("Existing manifest differs")
    path.write_bytes(data)
    print(json.dumps({"snapshotId": snapshot_id, "sourceSha256": digest(a),
                      "normalPages": page_count, "files": len(files) + 1,
                      "bytes": sum(value["bytes"] for value in files.values()) + len(data),
                      "httpVariantMeasured": False}, separators=(",", ":")))


if __name__ == "__main__":
    main()
