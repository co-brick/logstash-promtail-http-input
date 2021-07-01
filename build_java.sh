./gradlew clean build assemble generateGemJarRequiresFile vendor test

ruby -S bundle exec rspec ./spec/inputs/http_spec.rb:175

# in case of the error with bundler
## ruby -S  gem install bundler -v "$(grep -A 1 "BUNDLED WITH" Gemfile.lock | tail -n 1)"

# set LOGSTASH_PATH from logstash directory
## export LOGSTASH_PATH=`pwd`

# set JAVA_HOME to java 1.8
## export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_291.jdk/Contents/Home/