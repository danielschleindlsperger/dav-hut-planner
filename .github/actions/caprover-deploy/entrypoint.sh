#!/bin/sh -l

set -e

caprover deploy \
  --caproverUrl "${INPUT_CAPROVER_URL}" \
  --caproverPassword "${INPUT_PASSWORD}" \
  --caproverApp "${INPUT_APP_NAME}" \
  --imageName "${INPUT_IMAGE_NAME}"