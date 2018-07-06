FROM clojure:lein as builder
RUN mkdir /app
WORKDIR /app
ADD . /app
RUN lein uberjar

FROM java:8
EXPOSE 8080

COPY --from=builder /app/target/webapp_tk3.jar /webapp_tk3.jar

CMD java -cp /webapp_tk3.jar clojure.main -m webapp-tk3.core
