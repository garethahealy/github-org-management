FROM registry.access.redhat.com/ubi9/ubi-minimal:9.7-1778562320@sha256:12db9874bd753eb98b1ab3d840e75de5d6842ac0604fbd68c012adefe97140be

WORKDIR /work/

RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work

COPY --chown=1001:root --chmod=0755 target/*-runner /work/application

EXPOSE 8080
USER 1001

ENTRYPOINT ["./application"]
