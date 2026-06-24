FROM registry.access.redhat.com/ubi9/ubi-minimal:9.8-1782191395@sha256:850143255ee0d1915f09aaa09f6ed31f24086ba605c323badfbefa95b8c52b0e

WORKDIR /work/

RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work

COPY --chown=1001:root --chmod=0755 target/*-runner /work/application

EXPOSE 8080
USER 1001

ENTRYPOINT ["./application"]
