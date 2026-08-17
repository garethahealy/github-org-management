FROM registry.access.redhat.com/ubi9/ubi-minimal:9.8-1786987521@sha256:8eb2830d0936237fc13a1f2f7e45aecf90d69043380ad167fad0343632937f41

WORKDIR /work/

RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work

COPY --chown=1001:root --chmod=0755 target/*-runner /work/application

EXPOSE 8080
USER 1001

ENTRYPOINT ["./application"]
