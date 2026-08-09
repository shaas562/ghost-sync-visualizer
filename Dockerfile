FROM gradle:9.5.1-jdk25

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends glslang-tools \
    && rm -rf /var/lib/apt/lists/*
USER gradle

WORKDIR /workspace
COPY . .

CMD ["gradle", "--no-daemon", "verifyMilestone1"]
