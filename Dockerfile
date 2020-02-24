FROM hirokimatsumoto/alpine-openjdk-11
COPY ./build/libs/upstox-download-golden-copy-NSE-1.0-SNAPSHOT.jar /app/upstox-download-golden-copy-NSE.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/upstox-download-golden-copy-NSE.jar"]