FROM unprotocol/bok-gradle

RUN set -o errexit -o nounset \
#  Download and build java-bok
    && echo "git clone" \
    && git clone https://github.com/unprotocol/java-bok.git \
    && cd java-bok \
    && gradle build

# Change work directory
WORKDIR /java-bok

# open port 18888
EXPOSE 18888

