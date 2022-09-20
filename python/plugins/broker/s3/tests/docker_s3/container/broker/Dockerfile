FROM minio/minio:latest

EXPOSE 9000 9001

#VOLUME /data

COPY init.sh /usr/local/bin/
#ENTRYPOINT ["/bin/sh", "-c"]
ENTRYPOINT []
CMD ["/usr/local/bin/init.sh"]
