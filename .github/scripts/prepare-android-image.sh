#!/usr/bin/env bash
set -euo pipefail

if [ -z "${CI_ANDROID_IMAGE:-}" ]; then
  echo "::error title=Android image::CI_ANDROID_IMAGE is required."
  exit 1
fi

mkdir -p build/ci-diagnostics

compose=(docker compose -f docker-compose.yml -f docker-compose.ci.yml)
image_source="pulled"

if ! "${compose[@]}" pull android; then
  echo "::warning::Could not pull the configured Android image; building the CI image locally."
  docker build --pull \
    --build-arg SOFTWARE_ACCELERATION=false \
    --tag "${CI_ANDROID_IMAGE}" \
    -f docker/android/Dockerfile \
    .
  image_source="built-local"
fi

{
  echo "CI_ANDROID_IMAGE=${CI_ANDROID_IMAGE}"
  echo "IMAGE_SOURCE=${image_source}"
  docker image inspect "${CI_ANDROID_IMAGE}" --format 'IMAGE_ID={{.Id}}'
  docker image inspect "${CI_ANDROID_IMAGE}" --format 'REPO_DIGESTS={{json .RepoDigests}}'
} > build/ci-diagnostics/android-image.txt

echo "Android image ready from ${image_source}."
