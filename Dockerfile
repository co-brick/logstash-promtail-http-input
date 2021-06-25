FROM jruby:9.2.17.0-jdk8 as build

COPY . .
RUN gem install bundler:2.2.21
RUN ruby -S bundle install
RUN ruby -S bundle exec rake vendor
RUN gem build logstash-promtail-http-input.gemspec

FROM grafana/logstash-output-loki
COPY --from=build logstash-promtail-http-input-1.0.0-java.gem .
RUN /usr/share/logstash/bin/logstash-plugin install --no-verify --local logstash-promtail-http-input-1.0.0-java.gem
