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
# Render entrega la base como DATABASE_URL (postgresql://usuario:clave@servidor/base); Java necesita
# jdbc:postgresql://servidor/base. Si llega, se arma DB_URL con lo que va despues de la @ (usuario y
# clave llegan aparte, en DB_USER y DB_PASSWORD). Sin DATABASE_URL se usa DB_URL tal cual.
ENTRYPOINT ["sh", "-c", "if [ -n \"$DATABASE_URL\" ]; then export DB_URL=\"jdbc:postgresql://${DATABASE_URL##*@}\"; fi; exec java -jar /app/app.jar"]
