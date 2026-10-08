#!/usr/bin/env python3
"""Compile this variant with already installed Kotlin and existing dependency JARs.

No dependencies are downloaded. Shared A/E source files are only read.
Independent test authors can compile their own tests against build/classes.
"""

import argparse
import os
from pathlib import Path
import subprocess


VARIANT = Path(__file__).resolve().parent
ROOT = VARIANT.parent.parent
SHARED = ROOT / "conditions/E/app/src/main/java/com/example/kdoctest"
SHARED_FILES = [
    "domain/model/Product.kt", "domain/repository/ProductRepository.kt",
    "domain/usecase/LoadCatalogPage.kt", "data/dto/ProductDto.kt",
    "data/dto/ProductMapper.kt", "data/remote/CatalogJson.kt",
    "data/remote/CatalogCursor.kt", "data/remote/CatalogScenario.kt",
]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--kotlin-home", required=True, type=Path,
                        help="Existing Kotlin distribution directory containing bin/ and lib/.")
    parser.add_argument("--classpath", required=True,
                        help="Existing coroutines-core, serialization-json and serialization-core JARs.")
    parser.add_argument("--java-home", type=Path, help="Existing JDK installation, if needed.")
    args = parser.parse_args()
    compiler = args.kotlin_home / "bin/kotlinc"
    plugin = args.kotlin_home / "lib/kotlinx-serialization-compiler-plugin.jar"
    if not compiler.is_file() or not plugin.is_file():
        parser.error("Kotlin compiler or serialization compiler plugin is missing")
    jars = [Path(value) for value in args.classpath.split(os.pathsep)]
    if not jars or not all(value.is_file() for value in jars):
        parser.error("Every classpath entry must be an existing JAR file")
    sources = [SHARED / value for value in SHARED_FILES]
    sources += sorted((VARIANT / "src/main/kotlin").rglob("*.kt"))
    output = VARIANT / "build/classes"
    output.mkdir(parents=True, exist_ok=True)
    env = dict(os.environ)
    if args.java_home is not None:
        env["JAVA_HOME"] = str(args.java_home)
    command = [str(compiler), "-jvm-target", "11", "-classpath", args.classpath,
               "-Xplugin=" + str(plugin), *map(str, sources), "-d", str(output)]
    result = subprocess.run(command, cwd=VARIANT, env=env, check=False)
    return result.returncode


if __name__ == "__main__":
    raise SystemExit(main())
