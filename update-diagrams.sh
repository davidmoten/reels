#!/bin/bash
set -e
mvn clean install
cp target/generated-diagrams/*.svg src/docs/
