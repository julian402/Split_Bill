# Imagen del backend para el despliegue. Se construye en dos etapas:
# 1) compila el .jar con Maven (imagen grande, con JDK)
# 2) copia solo el .jar a una imagen pequena que trae solamente el JRE
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
# las pruebas se corren antes, en la maquina de desarrollo (necesitan Docker para Testcontainers)
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
# no se ejecuta como root
RUN useradd --system --uid 1001 splitbill
USER splitbill
COPY --from=build /app/target/splitbill-api-*.jar app.jar
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
