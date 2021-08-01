FROM clojure:openjdk-14-tools-deps-slim-buster as builder

WORKDIR /app

# add deps, change sometimes
COPY deps.edn /app/deps.edn
RUN clojure -P

# add sources files, change often
COPY resources /app/resources
COPY src/ /app/src

# build uberjar
RUN clojure -X:uberjar

##
## Clean base image for distribution
##

FROM openjdk:14-slim-buster

WORKDIR /app

# copy java artifact, changes every time
COPY --from=builder /app/target/dav-hut-planner.jar /app/app.jar

# set the command, with proper container support
CMD ["java","-XX:+UseContainerSupport","-XX:+UnlockExperimentalVMOptions","-jar","/app/app.jar"]