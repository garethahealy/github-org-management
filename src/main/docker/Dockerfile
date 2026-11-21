FROM registry.access.redhat.com/ubi9/ubi-minimal:9.9-1795198179@sha256:08d4c70f1baf6537a0728656cf7942d561e74a35916509d205b6519861266352

WORKDIR /work/

RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work

COPY --chown=1001:root --chmod=0755 target/*-runner /work/application

EXPOSE 8080
USER 1001

ENTRYPOINT ["./application"]
