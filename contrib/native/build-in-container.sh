docker run --rm \
  -v "${PWD}":/work \
  -w /work \
  oracle/graalvm-ce:20.1.0-java8 \
  bash -c "mkdir /opt/maven/ \\
  && curl -sSfL https://apache.lauf-forum.at/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz | tar zx -C /opt/maven/ \\
  && ln -s /opt/maven/apache-maven-*/bin/mvn /usr/local/bin/ \\
  && mvn -ntp -B clean package -DskipTests -Pnative"
