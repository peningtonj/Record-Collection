FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew :composeApp:jsBrowserDevelopmentExecutableDistribution --no-daemon

FROM caddy:2
COPY --from=builder /app/composeApp/build/dist/js/developmentExecutable /srv
COPY Caddyfile /etc/caddy/Caddyfile
EXPOSE 80
