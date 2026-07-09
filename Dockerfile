FROM registry.access.redhat.com/ubi9/ubi:latest

ARG REST_PORT=8080

ARG IMAGE_DISPLAY_NAME="Docker image with CDC CHCCLP REST server"
ARG IMAGE_DESCRIPTION="This image runs the CDC CHCCLP REST server which connects to CDC Access Server"

LABEL name="cdcchcclprestserver" \
      summary="${IMAGE_DISPLAY_NAME}" \
      description="${IMAGE_DESCRIPTION}" \
      io.k8s.display-name="${IMAGE_DISPLAY_NAME}" \
      io.k8s.description="${IMAGE_DESCRIPTION}"

# location of installation and instance directories
ENV HOME_DIR=/home/cdc
ENV CDC_HOME=/opt/cdc_chcclp_rest

# upgrade image and install required packages 
RUN yum -y upgrade
RUN yum -y install wget net-tools procps-ng nmap-ncat java-17-openjdk-devel maven

# Copying all files
WORKDIR ${CDC_HOME}
COPY . ${CDC_HOME}

RUN useradd -u 1100 -g 0 cdc && \
    chown -R cdc:root $CDC_HOME && \
    chmod -R g=u $CDC_HOME && \
    chmod g+rw /etc/passwd


USER 1100
WORKDIR ${CDC_HOME}

RUN ./build.sh

CMD ./run.sh

EXPOSE $REST_PORT
