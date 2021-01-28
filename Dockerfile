FROM unprotocol/bok-gradle

RUN set -o errexit -o nounset \
    && echo "git clone" \
    && git clone https://github.com/unprotocol/java-bok.git \
    && cd java-bok \
    && gradle build

WORKDIR /java-bok

EXPOSE 18888