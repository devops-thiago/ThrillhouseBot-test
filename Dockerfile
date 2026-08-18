FROM eclipse-temurin:21-jre

WORKDIR /app

# Built by `./gradlew installDist`.
COPY build/install/meterfold/lib /app/lib
COPY build/install/meterfold/bin /app/bin

# The price book is owned by finance and mounted by the deployment; it changes
# on its own cadence and is deliberately not baked into the image.
ENV METERFOLD_PRICE_BOOK=/etc/meterfold/price-book.properties
VOLUME ["/etc/meterfold"]

EXPOSE 8080

ENTRYPOINT ["/app/bin/meterfold"]
