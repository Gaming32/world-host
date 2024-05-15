./gradlew :$1:runTestHost --stacktrace &
host_pid = $!
./gradlew :$1:runTestJoiner --stacktrace &
joiner_pid = $!
wait $host_pid
code = $?
if [ "$code" -ne 0 ]; then
  exit $code
fi
exit $(wait $joiner_pid)
