## 前提条件

* docker engine の `experimental` を有効にする
* `qemu-user-static` などのクロスビルド環境をセットアップする


## ビルド手順

```
TAG=20210402

docker buildx build -t sinetstream/tutorial:${TAG}-amd64 --platform linux/amd64 --push amd64
docker buildx build -t sinetstream/tutorial:${TAG}-arm --platform linux/arm/v7 --push arm
docker manifest create sinetstream/tutorial:${TAG} sinetstream/tutorial:${TAG}-amd64 sinetstream/tutorial:${TAG}-arm
docker manifest push sinetstream/tutorial:${TAG}
```
