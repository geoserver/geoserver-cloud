FROM ubuntu:24.04 AS base

RUN apt-get update \
    && apt-get upgrade --assume-yes \
    && apt-get install --assume-yes --no-install-recommends \
    vim curl jq libmagic1 zip python3-pip libpq-dev python3-dev gcc \
    && rm -rf /var/lib/apt/lists/* \
    && rm /usr/lib/python*/EXTERNALLY-MANAGED

COPY . /acceptance_tests

WORKDIR /acceptance_tests
RUN python3 -m pip install --disable-pip-version-check .

COPY entrypoint.py /bin/entrypoint.py

ENV PYTHONUNBUFFERED=1
ENTRYPOINT ["/bin/entrypoint.py"]

HEALTHCHECK --interval=5s --start-period=15s --retries=20 CMD test -f /tmp/healthcheck || exit 1

CMD ["sleep", "infinity"]
