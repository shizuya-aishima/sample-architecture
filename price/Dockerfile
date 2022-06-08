FROM gradle:7.4.2-jdk17-jammy  AS build-env

RUN mkdir /price
WORKDIR /price
COPY . /price
# gcloud sdk install
# Installing the package

# ローカル用の設定
# RUN sed -i 's@archive.ubuntu.com@ftp.jaist.ac.jp/pub/Linux@g' /etc/apt/sources.list
RUN apt update && apt -y install netcat lsof gnupg2
RUN echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] http://packages.cloud.google.com/apt cloud-sdk main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list && curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key --keyring /usr/share/keyrings/cloud.google.gpg  add - && apt update -y && apt install google-cloud-sdk -y && \
  apt -y install google-cloud-sdk-firestore-emulator

RUN gradle build

FROM openjdk:17
COPY --from=build-env /price/build/libs/price-0.0.1-SNAPSHOT.jar /price.jar
ENTRYPOINT ["java","-jar", "/price.jar"]
