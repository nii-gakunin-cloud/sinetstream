#!/bin/sh
# sinetstream_cilのwriter/readerを動かして記録するスクリプト
set -ux

if [ "$(tmux ls | wc -l)" -ne 0 ];then
    echo "SOME TMUX SESSION EXIST"
    exit 1
fi

slp() {
    sleep "${SLP-2}"
}

CLI=${CLI-src/sinetstream_cli/sinetstream_cli.py}

init() {
    tmux new-session -d -x 1000 -y 10000
    slp
    tmux split
    for T in 0 1; do
        tmux send -t $T "PS1='$ '" ENTER
        tmux send -t $T "sinetstream_cli() { $CLI \"\$@\"; }"
    done
}

term() {
    tmux capture-pane -t 0 -p | cat -s > wlog.txt
    tmux capture-pane -t 1 -p | cat -s > rlog.txt
    tmux kill-session
}

banner() {
    for T in 0 1; do
        tmux send -t $T ENTER ": .................... $FN ...................." ENTER
    done
}

send_command_line() {
    FN=send_command_line banner
    tmux send -t 1 "sinetstream_cli read --text type=mqtt brokers=mqtt topic=test --count 1" ENTER
    slp
    tmux send -t 0 "sinetstream_cli write --text type=mqtt brokers=mqtt topic=test --message 'this is a test message.'" ENTER
}

service_spec() {
    FN=service_spec banner
    tmux send -t 1 "echo 'test-1:" ENTER
    tmux send -t 1 "    value_type: text" ENTER
    tmux send -t 1 "    type: mqtt" ENTER
    tmux send -t 1 "    brokers: mqtt" ENTER
    tmux send -t 1 "    topic: test' >.sinetstream_config.yml" ENTER
    tmux send -t 1 "sinetstream_cli read --service test-1 --text --count 1" ENTER
    slp
    tmux send -t 0 "sinetstream_cli write --service test-1 --text --message 'this is a test message.'" ENTER
    slp
    tmux send -t 1 "rm .sinetstream_config.yml" ENTER
}

send_file() {
    FN=send_file banner
    tmux send -t 1 "sinetstream_cli read --text type=mqtt brokers=mqtt topic=test --count 1" ENTER
    slp
    tmux send -t 0 "echo 'this is a test message.' >message.txt" ENTER
    tmux send -t 0 "sinetstream_cli write --text type=mqtt brokers=mqtt topic=test --file message.txt" ENTER
    tmux send -t 0 "rm message.txt" ENTER
}

recv_raw() {
    FN=recv_raw banner
    tmux send -t 1 "sinetstream_cli read --text type=mqtt brokers=mqtt topic=test --count 1 --raw" ENTER
    slp
    tmux send -t 0 "sinetstream_cli write --text type=mqtt brokers=mqtt topic=test --message 'this is a test message.'" ENTER
}

send_binary() {
    FN=send_binary banner
    tmux send -t 1 "sinetstream_cli read type=mqtt brokers=mqtt topic=test --count 1 --raw >receivd.bin" ENTER
    slp
    tmux send -t 0 "dd if=/dev/urandom bs=1024 count=16 of=message.bin" ENTER
    tmux send -t 0 "sinetstream_cli write type=mqtt brokers=mqtt topic=test <message.bin" ENTER
    slp
    tmux send -t 1 "openssl sha1 *.bin" ENTER
    tmux send -t 1 "rm message.bin receivd.bin" ENTER
}

save_file() {
    FN=save_file banner
    tmux send -t 1 "mkdir recved && sinetstream_cli read --text type=mqtt brokers=mqtt topic=test/sub --count 2 --file recved" ENTER
    slp
    tmux send -t 0 "sinetstream_cli write --text type=mqtt brokers=mqtt topic=test/sub --message 'this is the first message.'" ENTER
    tmux send -t 0 "sinetstream_cli write --text type=mqtt brokers=mqtt topic=test/sub --message 'this is the second message.'" ENTER
    slp
    tmux send -t 1 "ls -alF recved" ENTER
    tmux send -t 1 "cat recved/test%2Fsub-*-1" ENTER
    tmux send -t 1 "cat recved/test%2Fsub-*-2" ENTER
    tmux send -t 1 "rm -rf recved" ENTER
}

deep_param() {
    FN=vdeep_param banner
    tmux send -t 1 "sinetstream_cli read --text type=mqtt brokers=mqtt data_compression=true compression.algorithm=zstd topic=test --count 1" ENTER
    slp
    tmux send -t 0 "sinetstream_cli write --text type=mqtt brokers=mqtt data_compression=true compression.algorithm=zstd topic=test --message 'this is a test message.'" ENTER
}

value_yaml() {
    FN=value_yaml banner
    tmux send -t 1 "sinetstream_cli read --text type=kafka brokers='[kafka1,kafka2]' topic=test --count 1" ENTER
    slp
    slp
    tmux send -t 0 "sinetstream_cli write --text type=kafka brokers='[kafka1,kafka2]' topic=test --message 'this is a test message.'" ENTER
}

send_stdin_whole() {
    FN=send_stdin_whole banner
    tmux send -t 1 "sinetstream_cli read --text type=mqtt brokers=mqtt topic=test --count 1" ENTER
    slp
    tmux send -t 0 "echo 'aaa
bbb
ccc' | sinetstream_cli write --text type=mqtt brokers=mqtt topic=test" ENTER
}

send_stdin_line() {
    FN=send_stdin_line banner
    tmux send -t 1 "sinetstream_cli read --text type=mqtt brokers=mqtt topic=test --count 3" ENTER
    slp
    tmux send -t 0 "echo 'aaa
bbb
ccc' | sinetstream_cli write --text type=mqtt brokers=mqtt topic=test --line" ENTER
}

usecase_json() {
    FN=usecase_json banner
    tmux send -t 1 "sinetstream_cli read --text type=mqtt brokers=mqtt topic=test --count 2 --raw | while read X" ENTER
    tmux send -t 1 "do" ENTER
    tmux send -t 1 "    echo \"\$X\" | jq 'add'" ENTER
    tmux send -t 1 "done" ENTER
    slp
    tmux send -t 0 "echo '[1,
2,
3]' | jq -c | sinetstream_cli write --text type=mqtt brokers=mqtt topic=test" ENTER
    slp
    tmux send -t 0 "echo '[10,
20,
30]' | jq -c | sinetstream_cli write --text type=mqtt brokers=mqtt topic=test" ENTER
}


init; slp

send_command_line; slp
service_spec
send_file; slp
recv_raw; slp
send_binary; slp
save_file; slp
deep_param; slp
value_yaml; slp
send_stdin_whole; slp
send_stdin_line; slp
usecase_json; slp

term
vim -O wlog.txt rlog.txt
