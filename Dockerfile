# Use Alpine 3.20 as the base image for minimal size
FROM alpine:3.20

# Set environment variables to avoid user interaction during install
ENV DEBIAN_FRONTEND=noninteractive
ENV TERM=xterm

# Install dependencies and set up the virtual environment
RUN apk add --no-cache \
    curl \
    unzip \
    python3 \
    py3-pip \
    ca-certificates \
    bash \
    gnupg \
    && python3 -m venv /venv \
    && /venv/bin/pip install --upgrade pip \
    && /venv/bin/pip install awscli \
    && curl -sSL https://git.io/install-kubent | sh \
    && rm -rf /var/cache/apk/*  # Clean up to reduce image size

# Set the AWS CLI and kubent binaries to use from the virtual environment
ENV PATH="/venv/bin:$PATH"

# Set the ENTRYPOINT to kubent
ENTRYPOINT [ "/usr/local/bin/kubent" ]
