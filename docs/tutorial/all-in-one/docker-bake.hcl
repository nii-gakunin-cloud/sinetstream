variable "REGISTRY" {
  default = "harbor.vcloud.nii.ac.jp/sinetstream"
}

variable "VERSION" {
  default = "1.10.0"
}

variable "DATE" {
  default = formatdate("YYYYMMDD", timestamp())
}

// Strip optional "-N" suffix from VERSION (e.g. "1.10.0-2" -> "1.10.0")
function "base_version" {
  params = []
  result = element(split("-", VERSION), 0)
}

function "version_major" {
  params = []
  result = split(".", base_version())[0]
}

function "version_minor" {
  params = []
  result = join(".", slice(split(".", base_version()), 0, 2))
}

function "tutorial_tags" {
  params = []
  result = [
    "${REGISTRY}/tutorial:${version_major()}",
    "${REGISTRY}/tutorial:${version_minor()}",
    "${REGISTRY}/tutorial:${base_version()}",
    "${REGISTRY}/tutorial:${base_version()}-${DATE}",
  ]
}

function "ci_tags" {
  params = []
  result = [
    "${REGISTRY}/tutorial:ci-${DATE}",
  ]
}

# --- ローカルビルド ---

group "default" {
  targets = ["tutorial"]
}

target "tutorial" {
  dockerfile = "Dockerfile"
  target     = "tutorial"
  tags       = concat(tutorial_tags(), ["ss-tutorial:latest"])
  output     = ["type=docker"]
}

# --- レジストリへの push ---

target "ci" {
  dockerfile = "Dockerfile"
  target     = "ci"
  tags       = ci_tags()
}

target "ci-multiplatform" {
  inherits  = ["ci"]
  platforms = ["linux/amd64", "linux/arm64"]
}

target "tutorial-amd64" {
  dockerfile = "Dockerfile"
  target     = "tutorial"
  tags       = ["${REGISTRY}/tutorial:${base_version()}-${DATE}-amd64"]
  platforms  = ["linux/amd64"]
  output     = ["type=docker"]
}

target "tutorial-arm64" {
  dockerfile = "Dockerfile"
  target     = "tutorial"
  tags       = ["${REGISTRY}/tutorial:${base_version()}-${DATE}-arm64"]
  platforms  = ["linux/arm64"]
  output     = ["type=docker"]
}

target "tutorial-arm" {
  dockerfile = "Dockerfile.debian"
  target     = "tutorial"
  tags       = ["${REGISTRY}/tutorial:${base_version()}-${DATE}-arm"]
  platforms  = ["linux/arm"]
  output     = ["type=docker"]
}
