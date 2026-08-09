FROM gradle:9.5.1-jdk25

WORKDIR /workspace
COPY . .

CMD ["gradle", "--no-daemon", "verifyMilestone1"]
