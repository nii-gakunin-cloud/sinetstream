### コンテナイメージのビルド

### 前提条件

以下のコマンドを実行しておく
```console
docker run --privileged --rm tonistiigi/binfmt --install all
```

参考情報:

* https://docs.docker.com/build/building/multi-platform/

### ビルド手順

```
docker buildx build -t sinetstream/tutorial:${TAG}-amd64 --platform linux/amd64 --push .
docker buildx build -t sinetstream/tutorial:${TAG}-arm64 --platform linux/arm64 --push .
docker buildx build -t sinetstream/tutorial:${TAG}-arm --platform linux/arm/v7 --push -f Dockerfile-debian .
docker manifest create sinetstream/tutorial:${TAG} sinetstream/tutorial:${TAG}-amd64 sinetstream/tutorial:${TAG}-arm64 sinetstream/tutorial:${TAG}-arm
docker manifest push sinetstream/tutorial:${TAG}
```
