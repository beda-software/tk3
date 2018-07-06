FROM clojure:lein as builder
RUN mkdir /app
WORKDIR /app
ADD . /app
RUN lein uberjar

FROM java:8
EXPOSE 8080

COPY --from=builder /app/target/controller_tk3.jar /controller_tk3.jar

CMD java -cp /controller_tk3.jar clojure.main -m controller-tk3.core
