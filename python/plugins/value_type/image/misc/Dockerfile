FROM 31z4/tox

USER root

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive \
       apt-get install -y --no-install-recommends \
         libgtk2.0-dev libgl1-mesa-dev \
    && rm -rf /var/lib/apt/lists/*

USER tox
