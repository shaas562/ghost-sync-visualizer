FROM gradle:9.5.1-jdk25

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends glslang-tools \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /workspace \
    && chown -R gradle:gradle /workspace

WORKDIR /workspace
COPY --chown=gradle:gradle . .
USER gradle

CMD ["gradle", "--no-daemon", "verifyMilestone1"]
