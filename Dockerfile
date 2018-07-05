FROM clojure:lein as builder
RUN mkdir /app
WORKDIR /app
ADD . /app
RUN lein uberjar

FROM java:8
EXPOSE 8080

COPY --from=builder /app/target/tk3.jar /tk3.jar

CMD java -cp /tk3.jar clojure.main -m tk3.core
