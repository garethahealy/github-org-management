FROM registry.access.redhat.com/ubi9/ubi-minimal:9.8-1791808450@sha256:a21d19f72557aa893a0f6ca1177bb1ffd91df6015e60470a116c50a6a05efcd9

WORKDIR /work/

RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work

COPY --chown=1001:root --chmod=0755 target/*-runner /work/application

EXPOSE 8080
USER 1001

ENTRYPOINT ["./application"]
