FROM maven:3.6.0-jdk-8 as build
COPY . .
RUN mvn clean install

FROM gcr.io/distroless/java:11
COPY  scanr_enseignementsup-recherche_gouv_fr.crt /scanr.crt
COPY --from=build /target/springboot-import-module-0.0.1-SNAPSHOT.jar /app.jar
WORKDIR /
RUN [\
 "/usr/lib/jvm/java-11-openjdk-amd64/bin/keytool",\
 "-import",\
 "-trustcacerts",\
 "-cacerts",\
 "-noprompt",\
 "-storepass",\
 "changeit",\
 "-alias",\
 "my",\
 "-file",\
 "/scanr.crt"\
]
CMD ["app.jar"]
