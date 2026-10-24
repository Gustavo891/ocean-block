FROM eclipse-temurin:25-jre

WORKDIR /app
COPY build/install/textura-studio/lib/ /app/lib/
COPY web/dist/ /app/web/

ENV STUDIO_BIND=0.0.0.0 \
    TEXTURA_DATA=/data

EXPOSE 7070

ENTRYPOINT ["java", "-Djava.awt.headless=true", "-Dstudio.web=/app/web", "-cp", "/app/lib/*", "textura_studio.MainKt"]
